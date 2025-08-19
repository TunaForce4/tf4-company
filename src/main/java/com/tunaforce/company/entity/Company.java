package com.tunaforce.company.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "p_company")
public class Company extends Timestamped{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", unique = true, nullable = false, updatable = false)
    private UUID companyId;

    @NotBlank
    @Column(name = "company_name", unique = true, nullable = false, updatable = false)
    private String companyName;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType companyType;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @NotBlank
    @Column(nullable = false)
    private UUID hubId;

    private UUID userId;

    @Builder
    public Company(String companyName, String address, CompanyType companyType, UUID hubId, UUID userId) {
        this.companyName = companyName;
        this.address = address;
        this.companyType = companyType;
        this.hubId = hubId;
        this.userId = userId;
    }
}
