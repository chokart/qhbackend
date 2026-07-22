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
        // Inicializar Operadores con Código
        String[][] rawOperators = {
            {"72322", "MARIN VILLANUEVA, EDUARDO"},
            {"93606", "AGUILAR SARAVIA, ALBERTO"},
            {"10462", "ARENAS CHAVEZ, MARCEL"},
            {"94932", "CALIZAYA SALCEDO, JULIO"},
            {"21801", "FLOR PEÑALOZA, MANUEL"},
            {"94075", "HUANACO MACOAGA, JOSE"},
            {"10379", "HUAYTA PARICOTO, HILARIO"},
            {"93401", "JUAREZ PINTO, EDMUNDO"},
            {"99511", "LA JARA ANDIA, CESAR"},
            {"96431", "MAMANI MASCO, MIGUEL"},
            {"99146", "CALDERON MAQUERA, NESTOR"},
            {"96889", "PEÑALOZA PUMA, CELEDONIO"},
            {"96067", "QUISPE FLORES, JIMMY"},
            {"96247", "RADO LUNA, ISAUL"},
            {"103041", "CHAVEZ CABANILLAS JUAN NOEL"},
            {"10479", "ARROYO MADERA, LUIS"},
            {"96249", "BALLON VALDIVIA, NOE"},
            {"93085", "CHECCA QUISPE, HERNAN"},
            {"93610", "HUALLPA CALSIN, MARTIN"},
            {"94818", "HUAMANI CARDENAS, PABLO"},
            {"21864", "GALLARDO BRICEÑO EDMUNDO"},
            {"10899", "JUAREZ CHIPANA, VICTOR"},
            {"96291", "LIMA YANQUE, MIGUEL"},
            {"95349", "MANRIQUE TEJADA, LEONIDAS"},
            {"99519", "MAMANI FLORES, HENRY"},
            {"100476", "MAMANI RAMOS ROLANDO"},
            {"21816", "PAREDES FELICIANO, LEONARDO"},
            {"93608", "ROSPIGLIOSI MAMANI, VICTOR"},
            {"101669", "GONZA JAHUIRA EDGARD"},
            {"91168", "VEGA CALATAYUD, MOISES"},
            {"72323", "ASQUI JULI, FELIX"},
            {"92793", "AGUILAR AROS, FERNANDO"},
            {"21663", "ALVAREZ BAUTISTA, ROLANDO"},
            {"93108", "ALVAREZ CARMONA, ROSALIO"},
            {"10924", "ARANA ORTEGA, ELVIS"},
            {"93602", "CISNEROS FLORES, EDGAR"},
            {"101663", "CUTIPA ARCE VICTOR JESUS"},
            {"96242", "HURTADO GOMEZ, EDWIN"},
            {"11004", "LOVON GARCIA, ELMER"},
            {"96994", "PAREDES PEREZ, ROLANDO"},
            {"97080", "PEREZ NAHUINCHA, MILTON"},
            {"91851", "POMA CLEMENTE, JOEL"},
            {"93572", "RIOS VARGAS, YAIR"},
            {"93856", "RIVERA PINTO, MARIO"},
            {"95348", "ROMERO MACHACA, ROY"},
            {"98064", "TREBEJO BALDEON, DENNIS"},
            {"96261", "ALCOCER AYALA, EDWIN"},
            {"96073", "CHOQUEHUANCA CRUZ, LUCIO"},
            {"10911", "CHUCTAYA CHUCTAYA, GERMAN"},
            {"99143", "CHURA AYCACHI, JAVIER"},
            {"91167", "FARJE NAPA, JAVIER"},
            {"93317", "GARNICA PALMA, ABEL"},
            {"93940", "LUCANO HUACAN, CARLOS"},
            {"10971", "MENDOZA MOSCOSO, WILFREDO"},
            {"91383", "MONCADA CHAVEZ, JUAN"},
            {"97028", "PAREDES ARAGON, CARLOS"},
            {"93868", "ROSADO VALERIANO, JONATHAN"},
            {"98066", "AGUILAR MENDOZA, ELVIS"},
            {"99291", "TORRES HUAYNA, WILSON"},
            {"98476", "SIXTO QUISPE, ZAMIR"},
            {"92434", "LOZA QUISPE, OSWALDO"},
            {"104777", "NAVARRO RONDÓN, GUSTAVO LEANDRO"},
            {"98477", "MAMANI NINA, PEDRO"},
            {"101667", "MARON MARON, ISIDRO LUIS"},
            {"99147", "MULLAYA ESCARCENA, JULIO"},
            {"100493", "ORTIZ BALLON, DIEGO"},
            {"103979", "CUTIPA ARCE, EFRAIN"},
            {"104721", "CHARCA CHULLUNQUIA, FREDY"},
            {"98063", "APAZA MAMANI, JUAN"},
            {"101679", "ARANIBAR GALDOS, OSMAR JULIO"},
            {"101773", "BARRIGA LIZANA, JEAN PIERRE"},
            {"95346", "BEDREGAL BARRIGA, ALFREDO"},
            {"104717", "CHOQUEHUANCA ARTETA, LUIS ANTONIO"},
            {"102911", "CHAUPE CCAHUA ALEXANDER"},
            {"98060", "MAMANI PORTUGAL, JORGE"},
            {"92471", "QUIRPER JUAREZ, JOAN"},
            {"91470", "MALLMA ACUÑA, MIGUEL"},
            {"101915", "CARNERO LLERENA, DERLING"},
            {"96927", "ESQUICHE ROJAS, JUAN"},
            {"99502", "HUAYHUA CONDORI, ANGEL"},
            {"100495", "MAMANI RAMOS, LUZMARYED"},
            {"102909", "CCAPA MAGAÑO EDGAR"},
            {"99145", "PAREDES PERALTA, DAVID"},
            {"101361", "POMATANTA QUIROZ JUSTINIANO"},
            {"102912", "PAIMA VASQUEZ, NEWTON"},
            {"104719", "POZO CHUCHULLO, AMILCAR"},
            {"95575", "SALAS HUARCA, LUIS JOEL"},
            {"99515", "BASURTO VILLEGAS, JESUS"},
            {"104889", "SURCO CHOQUE CHARLES JHON"},
            {"104790", "CONZA ALARCON, ROSARIO MILAGROS"},
            {"102910", "CONDORI MAMANI FELIX"},
            {"102913", "PAREDES CRUZ MARCELINO"},
            {"92594", "MAMANI ALVAREZ, PEDRO"},
            {"95244", "MONTES SANCHEZ, BALTAZAR"},
            {"102890", "QUISPE VALERIANO, WASHINGTON"},
            {"101699", "APAZA TICONA ROGER"},
            {"103039", "CACYA PEREZ GABRIEL ARCANGEL"},
            {"98085", "CHUQUINAIRA SANA, MIGUEL"},
            {"102057", "CONDORCHOA RODRIGUEZ GELBER LUIS"},
            {"99348", "CUBA CUSIPUMA, ERICK LUIS"},
            {"104980", "HUARZA HUISA, JHONNY IVAN"},
            {"103384", "JARA CCOA, SANDRA LIZBET"},
            {"99432", "MENDOZA SALAS, WALTER"},
            {"99344", "TINTAYA QUISPE, HUGO MARCIAL"},
            {"98082", "CAPIA CONDORI, JAVIER ROGER"},
            {"104748", "CASTILLO QUINTANA, JOSSELIN"},
            {"101685", "CHOQUE CORDOVA, DAI EVANS"},
            {"101035", "LEEPHE CALDERON, MARCO"},
            {"101660", "MAMANI COAGUILA SANTOS GUILLERMO"},
            {"98295", "MAYHUA BARCENA, ERICK"},
            {"100400", "SOTO HUAMAN JAVIER"},
            {"98385", "VIZCARRA VARGAS, VIDAL"},
            {"99514", "ZEBALLOS AYALA, ELIZALDE"},
            {"100961", "AROCUTIPA CATACORA, JHON MANUEL"},
            {"91819", "AROAPAZA PALOMINO JULIO"},
            {"105116", "ARPASI PAUCAR, EDGAR"},
            {"97458", "CHOQUE MAMANI, WELMER"},
            {"95783", "MANIHUARI CARRERA, AGUSTIN"},
            {"98681", "NUÑONCCA YAULI, EBERTH"},
            {"103797", "MORALES ROSAS, EDWIN"},
            {"99387", "QUISPE TACORA, JAIME"},
            {"103311", "VALDEZ GONZALES, DANIEL ADRIÁN"},
            {"98580", "TRUJILLO OTAEGUI JUAN CARLOS"},
            {"96310", "CHAVEZ VALDIVIA HENRY PAUL"},
            {"96385", "GONZALES LIENDO LEONEL FABRIZZIO"},
            {"96841", "DIANDERAS ZAPANA EFRAIN"},
            {"97151", "SALCEDO ROSAS DANIEL"}
        };

        for (String[] opData : rawOperators) {
            String code = opData[0];
            String name = opData[1];
            java.util.Optional<com.suitech.qhbackend.model.Operator> existing = operatorRepository.findByName(name);
            if (existing.isPresent()) {
                com.suitech.qhbackend.model.Operator op = existing.get();
                if (op.getCode() == null || !op.getCode().equals(code)) {
                    op.setCode(code);
                    operatorRepository.save(op);
                }
            } else if (!operatorRepository.existsByCode(code)) {
                operatorRepository.save(new com.suitech.qhbackend.model.Operator(null, code, name));
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
