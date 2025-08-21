package com.tunaforce.company.dto.request;

import java.util.List;
import java.util.UUID;

public record CompanyIdListRequestDto(
        List<UUID> companyIdList
) {
}
