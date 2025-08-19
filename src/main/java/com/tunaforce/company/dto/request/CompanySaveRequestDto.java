package com.tunaforce.company.dto.request;

public record CompanySaveRequestDto(
        String companyName,
        String type,
        String address,
        String hubId,
        String userId
) {
}
