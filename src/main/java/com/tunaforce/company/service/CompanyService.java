package com.tunaforce.company.service;

import com.tunaforce.company.dto.request.CompanySaveRequestDto;
import com.tunaforce.company.dto.response.CompanyListResponseDto;
import com.tunaforce.company.dto.response.CompanyResponseDto;
import com.tunaforce.company.entity.Company;
import com.tunaforce.company.entity.CompanyType;
import com.tunaforce.company.exception.CompanyNotFoundException;
import com.tunaforce.company.repository.CompanyRepository;
import com.tunaforce.company.client.HubClient;
import com.tunaforce.company.client.dto.HubGetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            try {
                hubUUID = UUID.fromString(hubId);
            } catch (IllegalArgumentException ignored) {
            }
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

    @Transactional
    public void createCompany(@Valid CompanySaveRequestDto companySaveRequestDto) {
        UUID userId = null;
        if (companySaveRequestDto.userId() != null && !companySaveRequestDto.userId().isBlank()) {
            userId = UUID.fromString(companySaveRequestDto.userId());
        }

        companyRepository.save(Company.builder()
                .companyName(companySaveRequestDto.companyName())
                .address(companySaveRequestDto.address())
                .companyType(CompanyType.fromString(companySaveRequestDto.type()))
                .hubId(UUID.fromString(companySaveRequestDto.hubId()))
                .userId(userId)
                .build());
    }

    @Transactional
    public void editCompanyInfo(String companyId, @Valid CompanySaveRequestDto companySaveRequestDto,
                                String headerUserId, String headerUserRole) {
        Company company = companyRepository.findById(UUID.fromString(companyId)).orElseThrow(CompanyNotFoundException::new);

        // 권한: MASTER/HUB 이거나 소유자(userId 일치)
        boolean isPrivileged = headerUserRole != null && (
                headerUserRole.equalsIgnoreCase("MASTER") || headerUserRole.equalsIgnoreCase("HUB")
        );
        boolean isOwner = false;
        if (headerUserId != null && !headerUserId.isBlank() && company.getUserId() != null) {
            try {
                isOwner = company.getUserId().equals(UUID.fromString(headerUserId));
            } catch (IllegalArgumentException ignored) {}
        }
        if (!(isPrivileged || isOwner)) {
            throw new com.tunaforce.company.exception.ForbiddenException("수정 권한이 없습니다.");
        }
    // hubId는 필수(검증 어노테이션 존재). 값이 온 경우만 파싱 실패 방지
        UUID hubId = UUID.fromString(companySaveRequestDto.hubId());

        // userId는 선택 입력
        UUID userId = null;
        if (companySaveRequestDto.userId() != null && !companySaveRequestDto.userId().isBlank()) {
            userId = UUID.fromString(companySaveRequestDto.userId());
        }

        company.updateInfo(
                companySaveRequestDto.companyName(),
                companySaveRequestDto.address(),
                CompanyType.fromString(companySaveRequestDto.type()),
                hubId,
                userId
        );

    }

    @Transactional
    public void deleteCompany(String companyId, String headerUserId, String headerUserRole) {
        Company company = companyRepository.findById(UUID.fromString(companyId))
                .orElseThrow(CompanyNotFoundException::new);

    // 간단한 권한 체크: ADMIN/HUB는 모두 허용, 아니면 company.userId와 일치해야 함
    boolean isPrivileged = headerUserRole != null && (
        headerUserRole.equalsIgnoreCase("ADMIN") || headerUserRole.equalsIgnoreCase("HUB")
    );
        boolean isOwner = false;
        if (headerUserId != null && !headerUserId.isBlank() && company.getUserId() != null) {
            try {
                isOwner = company.getUserId().equals(UUID.fromString(headerUserId));
            } catch (IllegalArgumentException ignored) { /* 잘못된 UUID는 소유자 아님 */ }
        }

    if (!(isPrivileged || isOwner)) {
            throw new com.tunaforce.company.exception.ForbiddenException("삭제 권한이 없습니다.");
        }

        UUID deletedBy = null;
        if (headerUserId != null && !headerUserId.isBlank()) {
            try { deletedBy = UUID.fromString(headerUserId); } catch (IllegalArgumentException ignored) {}
        }
        company.delete(deletedBy);
    }
}
