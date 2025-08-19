package com.tunaforce.company.service;

import com.tunaforce.company.dto.response.CompanyListResponseDto;
import com.tunaforce.company.dto.response.CompanyResponseDto;
import com.tunaforce.company.entity.Company;
import com.tunaforce.company.exception.CompanyNotFoundException;
import com.tunaforce.company.repository.CompanyRepository;
import com.tunaforce.company.client.HubClient;
import com.tunaforce.company.client.dto.HubGetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public CompanyListResponseDto searchCompany(String name, String hubId) {
        // 빈 문자열은 null로 정규화 -> 전체 검색 동작 보장
        String normalizedName = (name == null || name.isBlank()) ? null : name;

        UUID hubUUID = null;
        if (hubId != null && !hubId.isBlank()) {
            try { hubUUID = UUID.fromString(hubId); } catch (IllegalArgumentException ignored) {}
        }

        List<Company> companies = companyRepository.search(normalizedName, hubUUID);

        // 요청 단위 로컬 캐시로 동일 hubId 중복 호출 방지
        Map<UUID, String> hubNameCache = new HashMap<>();

        List<CompanyResponseDto> items = companies.stream().map(c -> {
            String hubName = hubNameCache.computeIfAbsent(c.getHubId(), hid -> {
                try {
                    HubGetResponse hub = hubClient.getHub(hid);
                    return hub != null ? hub.hubName() : null;
                } catch (Exception ignored) {
                    return null;
                }
            });
            return new CompanyResponseDto(
                    c.getCompanyId(),
                    c.getCompanyName(),
                    c.getCompanyType().name(),
                    c.getAddress(),
                    hubName
            );
        }).collect(Collectors.toList());

        return new CompanyListResponseDto(items);
    }
}
