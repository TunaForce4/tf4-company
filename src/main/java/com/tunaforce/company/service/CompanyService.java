package com.tunaforce.company.service;

import com.tunaforce.company.dto.response.CompanyResponseDto;
import com.tunaforce.company.entity.Company;
import com.tunaforce.company.exception.CompanyNotFoundException;
import com.tunaforce.company.repository.CompanyRepository;
import com.tunaforce.company.client.HubClient;
import com.tunaforce.company.client.dto.HubGetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final HubClient hubClient;

    public CompanyResponseDto getCompanyInfo(String companyId) {
        Company company = companyRepository.findById(UUID.fromString(companyId))
                .orElseThrow(CompanyNotFoundException::new);

        String hubName = null;
        try {
            HubGetResponse hub = hubClient.getHub(company.getHubId());
            hubName = hub != null ? hub.hubName() : null;
        } catch (Exception ignored) {
            // 허브 서비스 장애 시 hubName은 null 유지
        }
        return new CompanyResponseDto(
                company.getCompanyId(),
                company.getCompanyName(),
                company.getCompanyType().name(),
                company.getAddress(),
                hubName
        );
    }
}
