package com.brewlab.domain.model;

import com.brewlab.domain.format.Formats;

import java.time.Instant;
import java.time.LocalDate;

/** Identidad y trazabilidad del lote de café verde ({@code CoffeeLot}). */
public final class CoffeeLot {
    private final String id;
    private final String userId;
    private final String code;
    private final String name;
    private final String country;
    private final String region;
    private final String municipality;
    private final String farm;
    private final String producer;
    private final String species;
    private final String variety;
    private final String process;
    private final Integer altitudeMin;
    private final Integer altitudeMax;
    private final String harvest;
    private final LocalDate receivedOn;
    private final Double moisturePct;
    private final Double density;
    private final String screenSize;
    private final String notes;
    private final boolean demo;
    private final Instant archivedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CoffeeLot(String id, String userId, String code, String name,
                     String country, String region, String municipality,
                     String farm, String producer, String species, String variety, String process,
                     Integer altitudeMin, Integer altitudeMax, String harvest, LocalDate receivedOn,
                     Double moisturePct, Double density, String screenSize, String notes,
                     boolean demo, Instant archivedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.code = code;
        this.name = name;
        this.country = country;
        this.region = region;
        this.municipality = municipality;
        this.farm = farm;
        this.producer = producer;
        this.species = species;
        this.variety = variety;
        this.process = process;
        this.altitudeMin = altitudeMin;
        this.altitudeMax = altitudeMax;
        this.harvest = harvest;
        this.receivedOn = receivedOn;
        this.moisturePct = moisturePct;
        this.density = density;
        this.screenSize = screenSize;
        this.notes = notes;
        this.demo = demo;
        this.archivedAt = archivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getRegion() {
        return region;
    }

    public String getMunicipality() {
        return municipality;
    }

    public String getFarm() {
        return farm;
    }

    public String getProducer() {
        return producer;
    }

    public String getSpecies() {
        return species;
    }

    public String getVariety() {
        return variety;
    }

    public String getProcess() {
        return process;
    }

    public Integer getAltitudeMin() {
        return altitudeMin;
    }

    public Integer getAltitudeMax() {
        return altitudeMax;
    }

    public String getHarvest() {
        return harvest;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public Double getMoisturePct() {
        return moisturePct;
    }

    public Double getDensity() {
        return density;
    }

    public String getScreenSize() {
        return screenSize;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isDemo() {
        return demo;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Altitud formateada ("1750 m", "1700–1900 m" o "—" si es desconocida). */
    public String getAltitudeDisplay() {
        if (altitudeMin == null && altitudeMax == null) {
            return Formats.UNKNOWN;
        }
        if (altitudeMin != null && altitudeMax != null && !altitudeMin.equals(altitudeMax)) {
            return altitudeMin + "–" + altitudeMax + "\u00a0m";
        }
        Integer v = altitudeMin != null ? altitudeMin : altitudeMax;
        return v + "\u00a0m";
    }

    public String getOriginDisplay() {
        StringBuilder sb = new StringBuilder();
        if (country != null && !country.isEmpty()) {
            sb.append(country);
        }
        if (region != null && !region.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(region);
        }
        if (municipality != null && !municipality.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(municipality);
        }
        return sb.length() == 0 ? Formats.UNKNOWN : sb.toString();
    }

    public String getMoistureDisplay() {
        return Formats.withUnit(moisturePct, 1, "%");
    }

    public String getDensityDisplay() {
        return Formats.withUnit(density, 0, "g/L");
    }
}
