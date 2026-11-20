package com.example.distribuidora.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


//TODO VER SI ES NECESARIO TANTO QUILOMBO PARA LA TRADUCCIÓN
public enum ProductUnit {
    UNIT("unidad", "unit"),
    KILOGRAM("kg", "kilogramo", "kilo"),
    GRAM("g", "gramo"),
    LITER("lt", "liter", "litro"),
    BOX("caja", "box"),
    PACK("pack");

    private final String apiValue;
    private final Set<String> aliases;

    ProductUnit(String apiValue, String... aliases) {
        this.apiValue = apiValue;
        this.aliases = Stream.concat(Stream.of(apiValue), Arrays.stream(aliases))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
    }

    @JsonCreator
    public static ProductUnit fromJson(String value) {
        return fromValue(value);
    }

    public static ProductUnit fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(unit -> unit.aliases.contains(normalized) || unit.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unidad no soportada: " + value));
    }

    @JsonValue
    public String toJson() {
        return apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }
}
