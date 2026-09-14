package com.suitech.qhbackend.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class QueryAnalyzerService {

    public enum Intent {
        INVENTARIO("Inventario de Documentos"),
        EPP("Requisitos de EPP (Equipos de Protección Personal)"),
        PELIGROS_RIESGOS("Matriz IPERC (Peligros y Riesgos)"),
        PROCEDIMIENTO_PASOS("Procedimiento Operativo PETS"),
        RESTRICCIONES("Restricciones y Controles Críticos"),
        REQUISITOS_SEGURIDAD("Estándares de Seguridad Ocupacional"),
        GENERAL("Consulta General ISO 45001");

        private final String label;
        Intent(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryAnalysis {
        private Intent intent;
        private String intentLabel;
        private List<String> preferredCategories;
        private List<String> keywords;
        private boolean isInventoryQuery;
    }

    private static final List<String> EPP_KEYWORDS = Arrays.asList(
            "epp", "epps", "casco", "lentes", "guantes", "tapones", "orejeras", "respirador", 
            "barbiquejo", "chaleco", "botas", "arnes", "arnés", "linea de vida", "línea de vida", 
            "protección personal", "proteccion personal", "careta", "traje tyvek"
    );

    private static final List<String> PELIGRO_KEYWORDS = Arrays.asList(
            "peligro", "peligros", "riesgo", "riesgos", "iperc", "matriz", "evaluacion", 
            "evaluación", "consecuencia", "evaluación de riesgo", "medida de control", "severidad", "probabilidad"
    );

    private static final List<String> PASOS_KEYWORDS = Arrays.asList(
            "pets", "paso", "pasos", "procedimiento", "como se hace", "cómo se hace", 
            "secuencia", "ejecucion", "ejecución", "instruccion", "instrucción", "metodologia", 
            "termofusion", "termofusión", "canchas", "dique", "tubería", "tuberia", "hdpe", "pega"
    );

    private static final List<String> RESTRICCION_KEYWORDS = Arrays.asList(
            "prohibido", "prohibicion", "prohibición", "restriccion", "restricción", "no se debe", 
            "detencion", "detención", "bloqueo", "etiquetado", "loto", "tormenta", "lluvia", "viento",
            "permiso de trabajo", "petar"
    );

    private static final List<String> INVENTARIO_KEYWORDS = Arrays.asList(
            "inventario", "lista de documentos", "que documentos", "qué documentos", "cuantos documentos",
            "cuántos documentos", "archivos subidos", "lista de pets", "lista de iperc"
    );

    public QueryAnalysis analyzeQuery(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return QueryAnalysis.builder()
                    .intent(Intent.GENERAL)
                    .intentLabel(Intent.GENERAL.getLabel())
                    .preferredCategories(Collections.emptyList())
                    .keywords(Collections.emptyList())
                    .isInventoryQuery(false)
                    .build();
        }

        String lowerQuery = userQuery.toLowerCase().trim();
        List<String> keywords = extractKeywords(lowerQuery);

        // 1. Check Inventory
        for (String ik : INVENTARIO_KEYWORDS) {
            if (lowerQuery.contains(ik)) {
                return QueryAnalysis.builder()
                        .intent(Intent.INVENTARIO)
                        .intentLabel(Intent.INVENTARIO.getLabel())
                        .preferredCategories(Collections.emptyList())
                        .keywords(keywords)
                        .isInventoryQuery(true)
                        .build();
            }
        }

        // 2. Score intents
        int eppScore = countMatches(lowerQuery, EPP_KEYWORDS);
        int peligroScore = countMatches(lowerQuery, PELIGRO_KEYWORDS);
        int pasosScore = countMatches(lowerQuery, PASOS_KEYWORDS);
        int restriccionScore = countMatches(lowerQuery, RESTRICCION_KEYWORDS);

        Intent chosenIntent = Intent.GENERAL;
        List<String> preferredCategories = new ArrayList<>();

        if (eppScore > 0 && eppScore >= peligroScore && eppScore >= pasosScore) {
            chosenIntent = Intent.EPP;
            preferredCategories.addAll(Arrays.asList("PETS", "ESTANDAR_SEGURIDAD", "ESTANDAR_OPERATIVO"));
        } else if (peligroScore > 0 && peligroScore >= pasosScore) {
            chosenIntent = Intent.PELIGROS_RIESGOS;
            preferredCategories.addAll(Arrays.asList("IPERC", "ESTANDAR_SEGURIDAD"));
        } else if (pasosScore > 0) {
            chosenIntent = Intent.PROCEDIMIENTO_PASOS;
            preferredCategories.addAll(Arrays.asList("PETS", "ESTANDAR_OPERATIVO"));
        } else if (restriccionScore > 0) {
            chosenIntent = Intent.RESTRICCIONES;
            preferredCategories.addAll(Arrays.asList("PETS", "ESTANDAR_SEGURIDAD", "LEYES"));
        }

        log.info("Query Analyzer -> Query: '{}' | Intent: {} | Preferred Categories: {}", userQuery, chosenIntent, preferredCategories);

        return QueryAnalysis.builder()
                .intent(chosenIntent)
                .intentLabel(chosenIntent.getLabel())
                .preferredCategories(preferredCategories)
                .keywords(keywords)
                .isInventoryQuery(false)
                .build();
    }

    private int countMatches(String query, List<String> keywordList) {
        int count = 0;
        for (String kw : keywordList) {
            if (query.contains(kw)) {
                count++;
            }
        }
        return count;
    }

    private List<String> extractKeywords(String query) {
        String clean = query.replaceAll("[^a-z0-9áéíóúñ]", " ");
        String[] tokens = clean.split("\\s+");
        List<String> result = new ArrayList<>();
        for (String t : tokens) {
            if (t.length() > 2 && !isStopWord(t)) {
                result.add(t);
            }
        }
        return result;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "para", "como", "esta", "este", "estos", "estas", "cual", "cuales", "cuál", "cuáles",
                "donde", "dónde", "cuando", "cuándo", "quien", "quién", "porque", "porqué", "sobre",
                "entre", "hacia", "hasta", "desde", "según", "segun", "todo", "toda", "todos", "todas"
        ));
        return stopWords.contains(word);
    }
}
