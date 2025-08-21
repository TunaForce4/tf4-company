package com.tunaforce.company.dto.response;

import java.util.UUID;

public record CompanyResponseDto(
        UUID companyId,
        String companyName,
        String companyType,
        String address,
        UUID hubId,
        String hubName
) {
}
