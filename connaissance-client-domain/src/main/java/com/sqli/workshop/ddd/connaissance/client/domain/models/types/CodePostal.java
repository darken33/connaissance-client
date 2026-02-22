package com.sqli.workshop.ddd.connaissance.client.domain.models.types;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CodePostal(
                @NotNull 
                @Pattern(regexp = "^[A-Z0-9]+$") 
                @Size(min = 5, max = 5) 
                String value
            ) implements Comparable<CodePostal> {
    @Override
    public int compareTo(CodePostal o) {
        return value().compareTo(o.value());
    }
}