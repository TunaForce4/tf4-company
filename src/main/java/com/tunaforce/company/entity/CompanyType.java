package com.tunaforce.company.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum CompanyType {
    PRODUCER("PRODUCER"),
    RECEIVER("RECEIVER");

    private final String type;

    CompanyType(String type) {
        this.type = type;
    }

    @JsonCreator
    public static CompanyType fromString(String type) {
        for (CompanyType companyType : CompanyType.values()) {
            if (companyType.type.equalsIgnoreCase(type)) {
                return companyType;
            }
        }
        throw new IllegalArgumentException("Invalid Type: " + type );
    }
}
