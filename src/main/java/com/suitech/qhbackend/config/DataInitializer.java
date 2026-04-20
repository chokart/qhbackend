package com.suitech.qhbackend.config;

import com.suitech.qhbackend.model.Equipment;
import com.suitech.qhbackend.model.Role;
import com.suitech.qhbackend.model.User;
import com.suitech.qhbackend.repository.EquipmentRepository;
import com.suitech.qhbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Inicializar Usuario Admin
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("quebrada"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println("Usuario Administrador inicial creado: admin / quebrada");
        }

        // Inicializar Equipos
        // YA NO USAMOS equipmentRepository.deleteAll() para no perder las coordenadas guardadas por el usuario
        
        List<String> equipmentNames = Arrays.asList(
                "D8T-1", "D8T-2", "D8T-3", "D8T-4", "D8T-5", "D8T-6",
                "D8-1", "D8-2", "D9-1", "D9-2", "D9-3", "D9-4", "D9-5",
                "D9T-1", "D9T-2", "D9T-3", "D10T-2", "D10T-3", "D10T-5", "D10T-4",
                "Exc. 324 DL1", "Exc. 324 DL2", "Exc. Kom PC-220", "Exc. 326 DL1", "Exc. 326 DL2", "Exc. 336-1", "Exc. 336-2",
                "Cargador 988 F3", "Cargador 994K CAT", "Motoniveladora 16H", "Retroexcavadora 45",
                "Rodillo #6", "Rodillo #7", "Rodillo #8", "Rodillo #9", "Rodillo #10", "Rodillo #11", "Rodillo #12",
                "Volquete #80", "Volquete #82", "Volquete #84",
                "BATERIA 1", "BATERIA 2", "BATERIA 3", "BATERIA 4", "BATERIA 5", "BATERIA 6", "BATERIA 7", "BATERIA 8",
                "NIDO12800", "NIDO22800", "NIDO12101", "NIDO22102"
            );

            int createdCount = 0;
            for (String name : equipmentNames) {
                // Solo insertamos si el equipo no existe por nombre
                if (!equipmentRepository.existsByName(name)) {
                    Equipment eq = Equipment.builder()
                            .name(name)
                            .latitude(-17.459974)
                            .longitude(-70.801105)
                            .color(getColorByCategory(name))
                            .status(com.suitech.qhbackend.model.EquipmentStatus.OPERATIVO)
                            .comment("Equipo inicializado por el sistema")
                            .lastUpdatedBy("System")
                            .build();
                    equipmentRepository.save(eq);
                    createdCount++;
                }
            }
            if (createdCount > 0) {
                System.out.println("Se han registrado " + createdCount + " nuevos equipos.");
            } else {
                System.out.println("No se requirieron nuevos equipos, la flota está al día.");
            }
    }

    private String getColorByCategory(String name) {
        if (name.startsWith("BATERIA") || name.startsWith("NIDO")) return "#00cec9"; // Cian para Hidrociclones
        if (name.startsWith("D8")) return "#ff4757"; // Rojo
        if (name.startsWith("D9")) return "#2ed573"; // Verde
        if (name.startsWith("D10")) return "#1e90ff"; // Azul
        if (name.contains("Exc.")) return "#ffa502"; // Naranja
        if (name.contains("Cargador")) return "#aa3bff"; // Morado
        if (name.contains("Rodillo")) return "#f1f2f6"; // Gris Plata Claro
        if (name.contains("Volquete")) return "#1e272e"; // Negro Profundo
        if (name.contains("Retroexcavadora")) return "#ff6b81"; // Rosa
        if (name.contains("Motoniveladora")) return "#00d2ff"; // Cian
        return "#3388ff"; // Azul estándar
    }

    private String getRandomColor() {
        String[] colors = {"#ff4757", "#2ed573", "#1e90ff", "#ffa502", "#aa3bff", "#747d8c", "#2f3542"};
        return colors[(int) (Math.random() * colors.length)];
    }
}
