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
    private final com.suitech.qhbackend.repository.CanchaRepository canchaRepository;
    private final com.suitech.qhbackend.repository.CanchaCapaRepository canchaCapaRepository;
    private final com.suitech.qhbackend.repository.OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Inicializar Operadores
        List<String> operators = Arrays.asList(
            "MARIN VILLANUEVA, EDUARDO", "AGUILAR SARAVIA, ALBERTO", "ARENAS CHAVEZ, MARCEL", "CALIZAYA SALCEDO, JULIO",
            "FLOR PEÑALOZA, MANUEL", "HUANACO MACOAGA, JOSE", "HUAYTA PARICOTO, HILARIO", "JUAREZ PINTO, EDMUNDO",
            "LA JARA ANDIA, CESAR", "MAMANI MASCO, MIGUEL", "CALDERON MAQUERA, NESTOR", "PEÑALOZA PUMA, CELEDONIO",
            "QUISPE FLORES, JIMMY", "RADO LUNA, ISAUL", "CHAVEZ CABANILLAS JUAN NOEL", "ARROYO MADERA, LUIS",
            "BALLON VALDIVIA, NOE", "CHECCA QUISPE, HERNAN", "HUALLPA CALSIN, MARTIN", "HUAMANI CARDENAS, PABLO",
            "GALLARDO BRICEÑO EDMUNDO", "JUAREZ CHIPANA, VICTOR", "LIMA YANQUE, MIGUEL", "MANRIQUE TEJADA, LEONIDAS",
            "MAMANI RAMOS ROLANDO", "MAMANI FLORES, HENRY", "PAREDES FELICIANO, LEONARDO", "ROSPIGLIOSI MAMANI, VICTOR",
            "GONZA JAHUIRA EDGARD", "TINTAYA QUISPE, HUGO MARCIAL", "VEGA CALATAYUD, MOISES", "ASQUI JULI, FELIX",
            "AGUILAR AROS, FERNANDO", "ALVAREZ BAUTISTA, ROLANDO", "ALVAREZ CARMONA, ROSALIO", "ARANA ORTEGA, ELVIS",
            "CISNEROS FLORES, EDGAR", "CUTIPA ARE VICTOR JESUS", "HURTADO GOMEZ, EDWIN", "LOVON GARCIA, ELMER",
            "MAMANI RAMOS, LUZMARYED", "PAREDES PEREZ, ROLANDO", "PEREZ NAHUINCHA, MILTON", "POMA CLEMENTE, JOEL",
            "RIOS VARGAS, YAIR", "RIVERA PINTO, MARIO", "ROMERO MACHACA, ROY", "TREBEJO BALDEON, DENNIS",
            "ALCOCER AYALA, EDWIN", "CHOQUEHUANCA CRUZ, LUCIO", "CHUCTAYA CHUCTAYA, GERMAN", "CHURA AYCACHI, JAVIER",
            "FARJE NAPA, JAVIER", "GARNICA PALMA, ABEL", "LUCANO HUACAN, CARLOS", "MENDOZA MOSCOSO, WILFREDO",
            "MONCADA CHAVEZ, JUAN", "PAREDES ARAGON, CARLOS", "ROSADO VALERIANO, JONATHAN", "AGUILAR MENDOZA, ELVIS",
            "TORRES HUAYNA, WILSON", "SIXTO QUISPE, ZAMIR", "LOZA QUISPE, OSWALDO", "NAVARRO RONDÓN, GUSTAVO LEANDRO",
            "MAMANI NINA, PEDRO", "MARON MARON, ISIDRO LUIS", "MULLAYA ESCARCENA, JULIO", "ORTIZ BALLON, DIEGO",
            "CUTIPA ARCE, EFRAIN", "CHARCA CHULLUNQUIA, FREDY", "APAZA MAMANI, JUAN", "ARANIBAR GALDOS, OSMAR JULIO",
            "BARRIGA LIZANA, JEAN PIERRE", "BEDREGAL BARRIGA, ALFREDO", "CHOQUEHUANCA ARTETA, LUIS ANTONIO",
            "CHAUPE CCAHUA ALEXANDER", "MAMANI PORTUGAL, JORGE", "QUIRPER JUAREZ, JOAN", "MALLMA ACUÑA, MIGUEL",
            "CARNERO LLERENA, DERLING", "ESQUICHE ROJAS, JUAN", "HUAYHUA CONDORI, ANGEL", "CCAPA MAGAÑO EDGAR",
            "PAREDES PERALTA, DAVID", "POMATANTA QUIROZ JUSTINIANO", "PAIMA VASQUEZ, NEWTON", "SALAS HUARCA, LUIS JOEL",
            "AROCUTIPA CATACORA, JHON MANUEL", "BASURTO VILLEGAS, JESUS", "CONZA ALARCON, ROSARIO MILAGROS",
            "PAREDES CRUZ MARCELINO", "POZO CHUCHULLO, AMILCAR", "MAMANI ALVAREZ, PEDRO", "MONTES SANCHEZ, BALTAZAR",
            "QUISPE VALERIANO, WASHINGTON", "APAZA TICONA ROGER", "CACYA PEREZ GABRIEL ARCANGEL", "CHUQUINAIRA SANA, MIGUEL",
            "CONDORCHOA RODRIGUEZ GELBER LUIS", "CUBA CUSIPUMA, ERICK LUIS", "JARA CCOA, SANDRA LIZBET",
            "MENDOZA SALAS, WALTER", "CAPIA CONDORI, JAVIER ROGER", "CASTILLO QUINTANA, JOSSELIN", "CHOQUE CORDOVA, DAI EVANS",
            "LEEPHE CALDERON, MARCO", "MAMANI COAGUILA SANTOS GUILLERMO", "MAYHUA BARCENA, ERICK", "SOTO HUAMAN JAVIER",
            "VIZCARRA VARGAS, VIDAL", "ZEBALLOS AYALA, ELIZALDE", "AROAPAZA PALOMINO JULIO", "CHOQUE MAMANI, WELMER",
            "MANIHUARI CARRERA, AGUSTIN", "NUÑONCCA YAULI, EBERTH", "CONDORI MAMANI FELIX", "GOULDING SARMIENTO JUAN",
            "MORALES ROSAS, EDWIN", "QUISPE TACORA, JAIME", "VALDEZ GONZALES, DANIEL ADRIÁN"
        );

        for (String opName : operators) {
            if (!operatorRepository.existsByName(opName)) {
                operatorRepository.save(new com.suitech.qhbackend.model.Operator(null, opName));
            }
        }

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

        // Inicializar Canchas (1 a 30)
        int canchasCreated = 0;
        for (int i = 1; i <= 30; i++) {
            if (!canchaRepository.existsByNumber(i)) {
                com.suitech.qhbackend.model.Cancha cancha = com.suitech.qhbackend.model.Cancha.builder()
                        .number(i)
                        .currentHeight(1050.0) // Altura inicial por defecto
                        .status(com.suitech.qhbackend.model.CanchaStatus.STAND_BY)
                        .comment("Cancha inicializada por el sistema")
                        .lastUpdatedBy("System")
                        .build();
                canchaRepository.save(cancha);
                canchasCreated++;
            }
        }
        if (canchasCreated > 0) {
            System.out.println("Se han inicializado " + canchasCreated + " canchas.");
        }

        // Inicializar Canchas por Capas (1 a 30)
        int canchasCapasCreated = 0;
        for (int i = 1; i <= 30; i++) {
            if (!canchaCapaRepository.existsByNumber(i)) {
                com.suitech.qhbackend.model.CanchaCapa canchaCapa = com.suitech.qhbackend.model.CanchaCapa.builder()
                        .number(i)
                        .currentCapa(1) // Capa inicial por defecto
                        .status(com.suitech.qhbackend.model.CanchaStatus.STAND_BY)
                        .comment("Cancha por capa inicializada por el sistema")
                        .lastUpdatedBy("System")
                        .build();
                canchaCapaRepository.save(canchaCapa);
                canchasCapasCreated++;
            }
        }
        if (canchasCapasCreated > 0) {
            System.out.println("Se han inicializado " + canchasCapasCreated + " canchas por capas.");
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
