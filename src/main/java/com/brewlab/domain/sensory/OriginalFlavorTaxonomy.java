package com.brewlab.domain.sensory;

import com.brewlab.domain.model.SensoryDescriptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Taxonomía sensorial jerárquica propia y versionada de BrewLab ({@code brewlab-v1.0}).
 *
 * <p>Licencia y atribución: vocabulario original diseñado específicamente para BrewLab
 * (Familia → Subfamilia → Descriptor). No copia ni traza la rueda oficial SCA/WCR
 * (cuya licencia es CC BY-NC-ND 4.0).
 */
public final class OriginalFlavorTaxonomy {
    public static final String VERSION = SensoryDescriptor.TAXONOMY_VERSION_V1;
    public static final String ATTRIBUTION =
            "Taxonomía Sensorial Original BrewLab v1.0 (Dominio propio del laboratorio; no derivada del gráfico SCA/WCR)";

    private static final Instant SEED_EPOCH = Instant.parse("2026-01-01T00:00:00Z");

    private OriginalFlavorTaxonomy() {
    }

    /** Devuelve los descriptores base de la versión {@code brewlab-v1.0}. */
    public static List<SensoryDescriptor> defaultDescriptors() {
        return Collections.unmodifiableList(Arrays.asList(
                // 1. Floral y Botánico
                desc("bl-fl-jazmin", "Floral y Botánico", "Flores blancas", "Jazmín fresco", "#D9779B"),
                desc("bl-fl-azahar", "Floral y Botánico", "Flores blancas", "Flor de cafeto / Azahar", "#D9779B"),
                desc("bl-fl-rosa", "Floral y Botánico", "Infusión floral", "Rosa mosqueta", "#C85A82"),
                desc("bl-fl-manzanilla", "Floral y Botánico", "Infusión floral", "Manzanilla dulce", "#C85A82"),
                desc("bl-fl-te-verde", "Floral y Botánico", "Herbal aromático", "Té verde sencha", "#9E6B8E"),
                desc("bl-fl-hierbaluisa", "Floral y Botánico", "Herbal aromático", "Hierbaluisa / Limoncillo", "#9E6B8E"),

                // 2. Frutal Luminoso
                desc("bl-fr-mandarina", "Frutal Luminoso", "Cítricos jugosos", "Mandarina madura", "#E88D2A"),
                desc("bl-fr-bergamota", "Frutal Luminoso", "Cítricos jugosos", "Bergamota", "#E88D2A"),
                desc("bl-fr-lima", "Frutal Luminoso", "Cítricos jugosos", "Lima dulce", "#E88D2A"),
                desc("bl-fr-durazno", "Frutal Luminoso", "Fruta de hueso", "Durazno amarillo", "#E06D3B"),
                desc("bl-fr-albaricoque", "Frutal Luminoso", "Fruta de hueso", "Albaricoque", "#E06D3B"),
                desc("bl-fr-mora", "Frutal Luminoso", "Bayas del bosque", "Mora andina", "#C94A58"),
                desc("bl-fr-arandano", "Frutal Luminoso", "Bayas del bosque", "Arándano silvestre", "#C94A58"),
                desc("bl-fr-maracuya", "Frutal Luminoso", "Pulpa tropical", "Maracuyá / Gulupa", "#E2A629"),
                desc("bl-fr-uchuva", "Frutal Luminoso", "Pulpa tropical", "Uchuva / Aguaymanto", "#E2A629"),

                // 3. Dulzor y Mieles
                desc("bl-du-panela", "Dulzor y Mieles", "Caña y melaza", "Panela de caña", "#C77D28"),
                desc("bl-du-melaza", "Dulzor y Mieles", "Caña y melaza", "Melaza tibia", "#C77D28"),
                desc("bl-du-caramelo", "Dulzor y Mieles", "Caramelizados", "Caramelo blando", "#B56A20"),
                desc("bl-du-miel", "Dulzor y Mieles", "Néctar apícola", "Miel multifloral", "#D4942A"),
                desc("bl-du-vainilla", "Dulzor y Mieles", "Néctar apícola", "Vaina de vainilla", "#D4942A"),

                // 4. Cacao y Tostados
                desc("bl-ca-nibs", "Cacao y Tostados", "Cacao puro", "Nibs de cacao 70%", "#6E473B"),
                desc("bl-ca-chocolate-leche", "Cacao y Tostados", "Cacao puro", "Chocolate con leche", "#6E473B"),
                desc("bl-ca-malta", "Cacao y Tostados", "Cereal tostado", "Malta dorada", "#855945"),
                desc("bl-ca-pan-corteza", "Cacao y Tostados", "Cereal tostado", "Corteza de pan artesanal", "#855945"),

                // 5. Frutos Secos y Semillas
                desc("bl-fs-almendra", "Frutos Secos y Semillas", "Nueces suaves", "Almendra tostada", "#9C6B43"),
                desc("bl-fs-avellana", "Frutos Secos y Semillas", "Nueces suaves", "Avellana", "#9C6B43"),
                desc("bl-fs-macadamia", "Frutos Secos y Semillas", "Semillas mantecosas", "Macadamia", "#AE7C52"),
                desc("bl-fs-sesamo", "Frutos Secos y Semillas", "Semillas mantecosas", "Sésamo dorado", "#AE7C52"),

                // 6. Especias Cálidas
                desc("bl-es-canela", "Especias Cálidas", "Corteza dulce", "Canela en rama", "#A84E32"),
                desc("bl-es-cardamomo", "Especias Cálidas", "Semilla aromática", "Cardamomo verde", "#944229"),
                desc("bl-es-clavo", "Especias Cálidas", "Semilla aromática", "Clavo de olor", "#944229"),

                // 7. Fermento y Vinoso
                desc("bl-fe-uva-tinta", "Fermento y Vinoso", "Maceración vinosa", "Mosto de uva tinta", "#7C3F6E"),
                desc("bl-fe-ciruela-pasa", "Fermento y Vinoso", "Maceración vinosa", "Ciruela pasa", "#7C3F6E"),
                desc("bl-fe-kombucha", "Fermento y Vinoso", "Ácido láctico / Sidra", "Manzana sidra / Kombucha", "#68365D")
        ));
    }

    private static SensoryDescriptor desc(String id, String family, String subfamily, String term, String colorHex) {
        return new SensoryDescriptor(id, null, family, subfamily, term, colorHex, VERSION, false, SEED_EPOCH);
    }

    /** Segmento geométrico SVG para renderizar la rueda concéntrica original en servidor. */
    public static final class WheelSector {
        private final String key;
        private final String parentKey;
        private final String label;
        private final String shortLabel;
        private final String colorHex;
        private final String pathData;
        private final double labelX;
        private final double labelY;
        private final double labelRotation;

        public WheelSector(String key, String parentKey, String label, String shortLabel,
                           String colorHex, String pathData, double labelX, double labelY, double labelRotation) {
            this.key = key;
            this.parentKey = parentKey;
            this.label = label;
            this.shortLabel = shortLabel;
            this.colorHex = colorHex;
            this.pathData = pathData;
            this.labelX = labelX;
            this.labelY = labelY;
            this.labelRotation = labelRotation;
        }

        public String getKey() { return key; }
        public String getParentKey() { return parentKey; }
        public String getLabel() { return label; }
        public String getShortLabel() { return shortLabel; }
        public String getColorHex() { return colorHex; }
        public String getPathData() { return pathData; }
        public double getLabelX() { return labelX; }
        public double getLabelY() { return labelY; }
        public double getLabelRotation() { return labelRotation; }
    }

    /** Construye los sectores del anillo interior (Familias) para un viewBox SVG de 520x520 (centro 260,260). */
    public static List<WheelSector> buildFamilySectors(List<SensoryDescriptor> descriptors) {
        Map<String, List<SensoryDescriptor>> byFamily = new LinkedHashMap<>();
        for (SensoryDescriptor d : descriptors) {
            List<SensoryDescriptor> list = byFamily.get(d.getFamily());
            if (list == null) {
                list = new ArrayList<>();
                byFamily.put(d.getFamily(), list);
            }
            list.add(d);
        }
        int total = Math.max(1, descriptors.size());
        List<WheelSector> sectors = new ArrayList<>();
        double startDeg = -90.0;
        for (Map.Entry<String, List<SensoryDescriptor>> entry : byFamily.entrySet()) {
            String family = entry.getKey();
            List<SensoryDescriptor> items = entry.getValue();
            double sweep = (items.size() * 360.0) / total;
            double endDeg = startDeg + sweep;
            String color = items.get(0).getColorHex();
            sectors.add(createSector(family, "", family, abbreviate(family, 15), color, 66.0, 148.0, startDeg, endDeg));
            startDeg = endDeg;
        }
        return sectors;
    }

    /** Construye los sectores del anillo exterior (Subfamilias) para un viewBox SVG de 520x520 (centro 260,260). */
    public static List<WheelSector> buildSubfamilySectors(List<SensoryDescriptor> descriptors) {
        Map<String, List<SensoryDescriptor>> bySub = new LinkedHashMap<>();
        for (SensoryDescriptor d : descriptors) {
            String key = d.getFamily() + "::" + d.getSubfamily();
            List<SensoryDescriptor> list = bySub.get(key);
            if (list == null) {
                list = new ArrayList<>();
                bySub.put(key, list);
            }
            list.add(d);
        }
        int total = Math.max(1, descriptors.size());
        List<WheelSector> sectors = new ArrayList<>();
        double startDeg = -90.0;
        for (Map.Entry<String, List<SensoryDescriptor>> entry : bySub.entrySet()) {
            List<SensoryDescriptor> items = entry.getValue();
            SensoryDescriptor first = items.get(0);
            double sweep = (items.size() * 360.0) / total;
            double endDeg = startDeg + sweep;
            sectors.add(createSector(
                    first.getSubfamily(),
                    first.getFamily(),
                    first.getSubfamily(),
                    abbreviate(first.getSubfamily(), 16),
                    first.getColorHex(),
                    152.0,
                    244.0,
                    startDeg,
                    endDeg
            ));
            startDeg = endDeg;
        }
        return sectors;
    }

    private static WheelSector createSector(String key, String parentKey, String label, String shortLabel,
                                            String colorHex, double rInner, double rOuter,
                                            double startDeg, double endDeg) {
        double cx = 260.0;
        double cy = 260.0;
        double gap = 0.8;
        double a1 = Math.toRadians(startDeg + gap);
        double a2 = Math.toRadians(Math.max(startDeg + gap + 1.0, endDeg - gap));

        double x1Out = cx + rOuter * Math.cos(a1);
        double y1Out = cy + rOuter * Math.sin(a1);
        double x2Out = cx + rOuter * Math.cos(a2);
        double y2Out = cy + rOuter * Math.sin(a2);

        double x2In = cx + rInner * Math.cos(a2);
        double y2In = cy + rInner * Math.sin(a2);
        double x1In = cx + rInner * Math.cos(a1);
        double y1In = cy + rInner * Math.sin(a1);

        int largeArc = (endDeg - startDeg) > 180.0 ? 1 : 0;
        String path = String.format(Locale.US,
                "M %.2f %.2f A %.2f %.2f 0 %d 1 %.2f %.2f L %.2f %.2f A %.2f %.2f 0 %d 0 %.2f %.2f Z",
                x1Out, y1Out, rOuter, rOuter, largeArc, x2Out, y2Out,
                x2In, y2In, rInner, rInner, largeArc, x1In, y1In);

        double midDeg = (startDeg + endDeg) / 2.0;
        double midRad = Math.toRadians(midDeg);
        double rMid = (rInner + rOuter) / 2.0;
        double lx = cx + rMid * Math.cos(midRad);
        double ly = cy + rMid * Math.sin(midRad);
        double rot = midDeg;
        if (rot > 90.0 && rot < 270.0) {
            rot -= 180.0;
        }
        return new WheelSector(key, parentKey, label, shortLabel, colorHex, path, lx, ly, rot);
    }

    private static String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}
