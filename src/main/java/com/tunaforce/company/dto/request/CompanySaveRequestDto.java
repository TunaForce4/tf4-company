package com.tunaforce.company.dto.request;

import java.util.UUID;

public record CompanySaveRequestDto(
        String companyName,
        String type,
        String address,
        UUID hubId,
        UUID userId
) {
}
