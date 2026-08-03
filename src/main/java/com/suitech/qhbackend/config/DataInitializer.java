package com.suitech.qhbackend.config;

import com.suitech.qhbackend.model.Equipment;
import com.suitech.qhbackend.model.Group;
import com.suitech.qhbackend.model.Operator;
import com.suitech.qhbackend.model.Role;
import com.suitech.qhbackend.model.User;
import com.suitech.qhbackend.repository.EquipmentRepository;
import com.suitech.qhbackend.repository.GroupRepository;
import com.suitech.qhbackend.repository.OperatorRepository;
import com.suitech.qhbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.*;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final com.suitech.qhbackend.repository.CanchaRepository canchaRepository;
    private final com.suitech.qhbackend.repository.CanchaCapaRepository canchaCapaRepository;
    private final OperatorRepository operatorRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Inicializar 12 Guardias con sus programas y fechas de anclaje
        String p1Json = "[\"D\",\"D\",\"D\",\"D\",\"D\",\"L\",\"L\",\"L\",\"N\",\"N\",\"N\",\"L\",\"D\",\"D\",\"D\",\"D\",\"L\",\"L\",\"N\",\"N\",\"N\",\"N\",\"L\",\"L\",\"L\",\"L\",\"L\",\"L\"]";
        String p2Json = "[\"N\",\"N\",\"N\",\"N\",\"N\",\"L\",\"L\",\"L\",\"D\",\"D\",\"D\",\"D\",\"D\",\"L\",\"L\",\"L\",\"N\",\"N\",\"N\",\"N\",\"N\",\"L\",\"L\",\"L\",\"D\",\"D\",\"D\",\"D\",\"D\",\"L\",\"L\",\"L\",\"L\",\"N\",\"N\",\"N\",\"N\",\"L\",\"L\",\"L\",\"L\",\"D\",\"D\",\"D\",\"D\",\"L\",\"L\",\"L\",\"L\"]";

        Object[][] groupDefs = {
            {"GUARDIA 1", "#4f46e5", "PROGRAMA_1", LocalDate.of(2026, 7, 1), p1Json},
            {"GUARDIA 2", "#06b6d4", "PROGRAMA_1", LocalDate.of(2026, 7, 8), p1Json},
            {"GUARDIA 3", "#10b981", "PROGRAMA_1", LocalDate.of(2026, 7, 15), p1Json},
            {"GUARDIA 4", "#f59e0b", "PROGRAMA_1", LocalDate.of(2026, 7, 22), p1Json},
            {"GUARDIA 5", "#ec4899", "PROGRAMA_1", LocalDate.of(2026, 7, 5), p1Json},
            {"GUARDIA 6", "#8b5cf6", "PROGRAMA_1", LocalDate.of(2026, 7, 12), p1Json},
            {"GUARDIA 7", "#3b82f6", "PROGRAMA_2", LocalDate.of(2026, 7, 1), p2Json},
            {"GUARDIA 8", "#14b8a6", "PROGRAMA_2", LocalDate.of(2026, 7, 8), p2Json},
            {"GUARDIA 9", "#84cc16", "PROGRAMA_2", LocalDate.of(2026, 7, 15), p2Json},
            {"GUARDIA 10", "#eab308", "PROGRAMA_2", LocalDate.of(2026, 7, 22), p2Json},
            {"GUARDIA 11", "#f97316", "PROGRAMA_2", LocalDate.of(2026, 7, 29), p2Json},
            {"GUARDIA 12", "#ef4444", "PROGRAMA_2", LocalDate.of(2026, 7, 5), p2Json}
        };

        Map<String, Group> groupMap = new HashMap<>();
        for (Object[] gDef : groupDefs) {
            String gName = (String) gDef[0];
            Group group = groupRepository.findByName(gName).orElseGet(() ->
                groupRepository.save(Group.builder()
                    .name(gName)
                    .color((String) gDef[1])
                    .programType((String) gDef[2])
                    .startDate((LocalDate) gDef[3])
                    .patternJson((String) gDef[4])
                    .build())
            );
            groupMap.put(gName, group);
        }

        // 2. Inicializar Operadores con Código y asignación exacta de Guardia
        String[][] rawOperators = {
            {"72322", "MARIN VILLANUEVA, EDUARDO", "GUARDIA 1"},
            {"93606", "AGUILAR SARAVIA, ALBERTO", "GUARDIA 1"},
            {"10462", "ARENAS CHAVEZ, MARCEL", "GUARDIA 1"},
            {"94932", "CALIZAYA SALCEDO, JULIO", "GUARDIA 1"},
            {"21801", "FLOR PEÑALOZA, MANUEL", "GUARDIA 1"},
            {"94075", "HUANACO MACOAGA, JOSE", "GUARDIA 1"},
            {"10379", "HUAYTA PARICOTO, HILARIO", "GUARDIA 1"},
            {"93401", "JUAREZ PINTO, EDMUNDO", "GUARDIA 1"},
            {"99511", "LA JARA ANDIA, CESAR", "GUARDIA 1"},
            {"96431", "MAMANI MASCO, MIGUEL", "GUARDIA 1"},
            {"99146", "CALDERON MAQUERA, NESTOR", "GUARDIA 1"},
            {"96889", "PEÑALOZA PUMA, CELEDONIO", "GUARDIA 1"},
            {"96067", "QUISPE FLORES, JIMMY", "GUARDIA 1"},
            {"96247", "RADO LUNA, ISAUL", "GUARDIA 1"},
            {"103041", "CHAVEZ CABANILLAS JUAN NOEL", "GUARDIA 2"},
            {"10479", "ARROYO MADERA, LUIS", "GUARDIA 2"},
            {"96249", "BALLON VALDIVIA, NOE", "GUARDIA 2"},
            {"93085", "CHECCA QUISPE, HERNAN", "GUARDIA 2"},
            {"93610", "HUALLPA CALSIN, MARTIN", "GUARDIA 2"},
            {"94818", "HUAMANI CARDENAS, PABLO", "GUARDIA 2"},
            {"21864", "GALLARDO BRICEÑO EDMUNDO", "GUARDIA 2"},
            {"10899", "JUAREZ CHIPANA, VICTOR", "GUARDIA 2"},
            {"96291", "LIMA YANQUE, MIGUEL", "GUARDIA 2"},
            {"95349", "MANRIQUE TEJADA, LEONIDAS", "GUARDIA 2"},
            {"99519", "MAMANI FLORES, HENRY", "GUARDIA 2"},
            {"100476", "MAMANI RAMOS ROLANDO", "GUARDIA 2"},
            {"21816", "PAREDES FELICIANO, LEONARDO", "GUARDIA 2"},
            {"93608", "ROSPIGLIOSI MAMANI, VICTOR", "GUARDIA 2"},
            {"101669", "GONZA JAHUIRA EDGARD", "GUARDIA 2"},
            {"91168", "VEGA CALATAYUD, MOISES", "GUARDIA 2"},
            {"72323", "ASQUI JULI, FELIX", "GUARDIA 3"},
            {"92793", "AGUILAR AROS, FERNANDO", "GUARDIA 3"},
            {"21663", "ALVAREZ BAUTISTA, ROLANDO", "GUARDIA 3"},
            {"93108", "ALVAREZ CARMONA, ROSALIO", "GUARDIA 3"},
            {"10924", "ARANA ORTEGA, ELVIS", "GUARDIA 3"},
            {"93602", "CISNEROS FLORES, EDGAR", "GUARDIA 3"},
            {"101663", "CUTIPA ARCE VICTOR JESUS", "GUARDIA 3"},
            {"96242", "HURTADO GOMEZ, EDWIN", "GUARDIA 3"},
            {"11004", "LOVON GARCIA, ELMER", "GUARDIA 3"},
            {"96994", "PAREDES PEREZ, ROLANDO", "GUARDIA 3"},
            {"97080", "PEREZ NAHUINCHA, MILTON", "GUARDIA 3"},
            {"91851", "POMA CLEMENTE, JOEL", "GUARDIA 3"},
            {"93572", "RIOS VARGAS, YAIR", "GUARDIA 3"},
            {"93856", "RIVERA PINTO, MARIO", "GUARDIA 3"},
            {"95348", "ROMERO MACHACA, ROY", "GUARDIA 4"},
            {"98064", "TREBEJO BALDEON, DENNIS", "GUARDIA 4"},
            {"96261", "ALCOCER AYALA, EDWIN", "GUARDIA 4"},
            {"96073", "CHOQUEHUANCA CRUZ, LUCIO", "GUARDIA 4"},
            {"10911", "CHUCTAYA CHUCTAYA, GERMAN", "GUARDIA 4"},
            {"99143", "CHURA AYCACHI, JAVIER", "GUARDIA 4"},
            {"91167", "FARJE NAPA, JAVIER", "GUARDIA 4"},
            {"93317", "GARNICA PALMA, ABEL", "GUARDIA 4"},
            {"93940", "LUCANO HUACAN, CARLOS", "GUARDIA 4"},
            {"10971", "MENDOZA MOSCOSO, WILFREDO", "GUARDIA 4"},
            {"91383", "MONCADA CHAVEZ, JUAN", "GUARDIA 4"},
            {"97028", "PAREDES ARAGON, CARLOS", "GUARDIA 4"},
            {"93868", "ROSADO VALERIANO, JONATHAN", "GUARDIA 4"},
            {"98066", "AGUILAR MENDOZA, ELVIS", "GUARDIA 4"},
            {"99291", "TORRES HUAYNA, WILSON", "GUARDIA 4"},
            {"98476", "SIXTO QUISPE, ZAMIR", "GUARDIA 4"},
            {"92434", "LOZA QUISPE, OSWALDO", "GUARDIA 5"},
            {"104777", "NAVARRO RONDÓN, GUSTAVO LEANDRO", "GUARDIA 5"},
            {"98477", "MAMANI NINA, PEDRO", "GUARDIA 5"},
            {"101667", "MARON MARON, ISIDRO LUIS", "GUARDIA 5"},
            {"99147", "MULLAYA ESCARCENA, JULIO", "GUARDIA 5"},
            {"100493", "ORTIZ BALLON, DIEGO", "GUARDIA 5"},
            {"103979", "CUTIPA ARCE, EFRAIN", "GUARDIA 5"},
            {"104721", "CHARCA CHULLUNQUIA, FREDY", "GUARDIA 5"},
            {"98063", "APAZA MAMANI, JUAN", "GUARDIA 6"},
            {"101679", "ARANIBAR GALDOS, OSMAR JULIO", "GUARDIA 6"},
            {"101773", "BARRIGA LIZANA, JEAN PIERRE", "GUARDIA 6"},
            {"95346", "BEDREGAL BARRIGA, ALFREDO", "GUARDIA 6"},
            {"104717", "CHOQUEHUANCA ARTETA, LUIS ANTONIO", "GUARDIA 6"},
            {"102911", "CHAUPE CCAHUA ALEXANDER", "GUARDIA 6"},
            {"98060", "MAMANI PORTUGAL, JORGE", "GUARDIA 6"},
            {"92471", "QUIRPER JUAREZ, JOAN", "GUARDIA 6"},
            {"91470", "MALLMA ACUÑA, MIGUEL", "GUARDIA 6"},
            {"101915", "CARNERO LLERENA, DERLING", "GUARDIA 7"},
            {"96927", "ESQUICHE ROJAS, JUAN", "GUARDIA 7"},
            {"99502", "HUAYHUA CONDORI, ANGEL", "GUARDIA 7"},
            {"100495", "MAMANI RAMOS, LUZMARYED", "GUARDIA 7"},
            {"102909", "CCAPA MAGAÑO EDGAR", "GUARDIA 7"},
            {"99145", "PAREDES PERALTA, DAVID", "GUARDIA 7"},
            {"101361", "POMATANTA QUIROZ JUSTINIANO", "GUARDIA 7"},
            {"102912", "PAIMA VASQUEZ, NEWTON", "GUARDIA 7"},
            {"104719", "POZO CHUCHULLO, AMILCAR", "GUARDIA 7"},
            {"95575", "SALAS HUARCA, LUIS JOEL", "GUARDIA 7"},
            {"99515", "BASURTO VILLEGAS, JESUS", "GUARDIA 8"},
            {"104889", "SURCO CHOQUE CHARLES JHON", "GUARDIA 8"},
            {"104790", "CONZA ALARCON, ROSARIO MILAGROS", "GUARDIA 8"},
            {"102910", "CONDORI MAMANI FELIX", "GUARDIA 8"},
            {"102913", "PAREDES CRUZ MARCELINO", "GUARDIA 8"},
            {"92594", "MAMANI ALVAREZ, PEDRO", "GUARDIA 8"},
            {"95244", "MONTES SANCHEZ, BALTAZAR", "GUARDIA 8"},
            {"102890", "QUISPE VALERIANO, WASHINGTON", "GUARDIA 8"},
            {"101699", "APAZA TICONA ROGER", "GUARDIA 9"},
            {"103039", "CACYA PEREZ GABRIEL ARCANGEL", "GUARDIA 9"},
            {"98085", "CHUQUINAIRA SANA, MIGUEL", "GUARDIA 9"},
            {"102057", "CONDORCHOA RODRIGUEZ GELBER LUIS", "GUARDIA 9"},
            {"99348", "CUBA CUSIPUMA, ERICK LUIS", "GUARDIA 9"},
            {"104980", "HUARZA HUISA, JHONNY IVAN", "GUARDIA 9"},
            {"103384", "JARA CCOA, SANDRA LIZBET", "GUARDIA 9"},
            {"99432", "MENDOZA SALAS, WALTER", "GUARDIA 9"},
            {"99344", "TINTAYA QUISPE, HUGO MARCIAL", "GUARDIA 9"},
            {"98082", "CAPIA CONDORI, JAVIER ROGER", "GUARDIA 10"},
            {"104748", "CASTILLO QUINTANA, JOSSELIN", "GUARDIA 10"},
            {"101685", "CHOQUE CORDOVA, DAI EVANS", "GUARDIA 10"},
            {"101035", "LEEPHE CALDERON, MARCO", "GUARDIA 10"},
            {"101660", "MAMANI COAGUILA SANTOS GUILLERMO", "GUARDIA 10"},
            {"98295", "MAYHUA BARCENA, ERICK", "GUARDIA 10"},
            {"100400", "SOTO HUAMAN JAVIER", "GUARDIA 10"},
            {"98385", "VIZCARRA VARGAS, VIDAL", "GUARDIA 10"},
            {"99514", "ZEBALLOS AYALA, ELIZALDE", "GUARDIA 10"},
            {"100961", "AROCUTIPA CATACORA, JHON MANUEL", "GUARDIA 11"},
            {"91819", "AROAPAZA PALOMINO JULIO", "GUARDIA 11"},
            {"105116", "ARPASI PAUCAR, EDGAR", "GUARDIA 11"},
            {"97458", "CHOQUE MAMANI, WELMER", "GUARDIA 11"},
            {"95783", "MANIHUARI CARRERA, AGUSTIN", "GUARDIA 11"},
            {"98681", "NUÑONCCA YAULI, EBERTH", "GUARDIA 11"},
            {"103797", "MORALES ROSAS, EDWIN", "GUARDIA 11"},
            {"99387", "QUISPE TACORA, JAIME", "GUARDIA 11"},
            {"103311", "VALDEZ GONZALES, DANIEL ADRIÁN", "GUARDIA 11"},
            {"98580", "TRUJILLO OTAEGUI JUAN CARLOS", "GUARDIA 12"},
            {"96310", "CHAVEZ VALDIVIA HENRY PAUL", "GUARDIA 12"},
            {"96385", "GONZALES LIENDO LEONEL FABRIZZIO", "GUARDIA 12"},
            {"96841", "DIANDERAS ZAPANA EFRAIN", "GUARDIA 12"},
            {"97151", "SALCEDO ROSAS DANIEL", "GUARDIA 12"}
        };

        for (String[] opData : rawOperators) {
            String code = opData[0];
            String name = opData[1];
            String gName = opData[2];
            Group assignedGroup = groupMap.get(gName);

            Optional<Operator> existing = operatorRepository.findByName(name);
            if (existing.isPresent()) {
                Operator op = existing.get();
                op.setCode(code);
                op.setGroup(assignedGroup);
                operatorRepository.save(op);
            } else {
                operatorRepository.save(Operator.builder()
                        .code(code)
                        .name(name)
                        .group(assignedGroup)
                        .build());
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
        }

        // Inicializar Canchas (1 a 30)
        int canchasCreated = 0;
        for (int i = 1; i <= 30; i++) {
            if (!canchaRepository.existsByNumber(i)) {
                canchaRepository.save(com.suitech.qhbackend.model.Cancha.builder()
                        .number(i)
                        .currentHeight(1050.0)
                        .status(com.suitech.qhbackend.model.CanchaStatus.STAND_BY)
                        .comment("Cancha inicializada por el sistema")
                        .lastUpdatedBy("System")
                        .build());
                canchasCreated++;
            }
        }

        // Inicializar Canchas por Capas (1 a 30)
        for (int i = 1; i <= 30; i++) {
            if (!canchaCapaRepository.existsByNumber(i)) {
                canchaCapaRepository.save(com.suitech.qhbackend.model.CanchaCapa.builder()
                        .number(i)
                        .currentCapa(1)
                        .status(com.suitech.qhbackend.model.CanchaStatus.STAND_BY)
                        .comment("Cancha por capa inicializada por el sistema")
                        .lastUpdatedBy("System")
                        .build());
            }
        }

        // Inicializar Equipos (Solo si la tabla está completamente vacía)
        if (equipmentRepository.count() == 0) {
            List<String> equipmentNames = Arrays.asList(
                    "D8T-1", "D8T-2", "D8T-3", "D8T-4", "D8T-5", "D8T-6",
                    "D8-1", "D8-2", "D9-1", "D9-2", "D9-3", "D9-4", "D9-5",
                    "D9T-1", "D9T-2", "D9T-3", "D10T-2",
                    "Exc. 324 DL1", "Exc. 324 DL2", "Exc. Kom PC-220", "Exc. 326 DL1", "Exc. 326 DL2", "Exc. 336-2",
                    "Cargador 988 F3", "Cargador 994K CAT", "Motoniveladora 16H", "Retroexcavadora 45",
                    "Rodillo #6", "Rodillo #7", "Rodillo #8", "Rodillo #9", "Rodillo #10", "Rodillo #11", "Rodillo #12",
                    "Volquete #80", "Volquete #82", "Volquete #84",
                    "BATERIA 1", "BATERIA 2", "BATERIA 3", "BATERIA 4", "BATERIA 5", "BATERIA 6", "BATERIA 7", "BATERIA 8",
                    "NIDO12800", "NIDO22800", "NIDO12101", "NIDO22102"
            );

            for (String name : equipmentNames) {
                equipmentRepository.save(Equipment.builder()
                        .name(name)
                        .latitude(-17.459974)
                        .longitude(-70.801105)
                        .color(getColorByCategory(name))
                        .status(com.suitech.qhbackend.model.EquipmentStatus.OPERATIVO)
                        .comment("Equipo inicializado por el sistema")
                        .lastUpdatedBy("System")
                        .build());
            }
        }
    }

    private String getColorByCategory(String name) {
        if (name.startsWith("BATERIA") || name.startsWith("NIDO")) return "#00cec9";
        if (name.startsWith("D8")) return "#ff4757";
        if (name.startsWith("D9")) return "#2ed573";
        if (name.startsWith("D10")) return "#1e90ff";
        if (name.contains("Exc.")) return "#ffa502";
        if (name.contains("Cargador")) return "#aa3bff";
        if (name.contains("Rodillo")) return "#f1f2f6";
        if (name.contains("Volquete")) return "#1e272e";
        if (name.contains("Retroexcavadora")) return "#ff6b81";
        if (name.contains("Motoniveladora")) return "#00d2ff";
        return "#3388ff";
    }
}
