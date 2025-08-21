package com.tunaforce.company.service;

import com.tunaforce.company.dto.request.CompanyIdListRequestDto;
import com.tunaforce.company.dto.request.CompanySaveRequestDto;
import com.tunaforce.company.dto.response.CompanyListResponseDto;
import com.tunaforce.company.dto.response.CompanyResponseDto;
import com.tunaforce.company.entity.Company;
import com.tunaforce.company.entity.CompanyType;
import com.tunaforce.company.exception.CompanyNotFoundException;
import com.tunaforce.company.repository.CompanyRepository;
import com.tunaforce.company.client.HubClient;
import com.tunaforce.company.client.dto.HubByAdminResponse;
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

    public CompanyResponseDto getCompanyInfo(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(CompanyNotFoundException::new);
        String hubName = resolveHubName(company.getHubId());
        return new CompanyResponseDto(
                company.getCompanyId(),
                company.getCompanyName(),
                company.getCompanyType().name(),
                company.getAddress(),
                hubName
        );
    }

    public CompanyListResponseDto searchCompany(String name, UUID hubId) {
        // 빈 문자열은 null로 정규화 -> 전체 검색 동작 보장
        String normalizedName = (name == null || name.isBlank()) ? null : name;

        List<Company> companies = companyRepository.search(normalizedName, hubId);

        return toCompanyListResponse(companies);
    }

    // 역할 기반 조회 범위 강제 버전 (Controller에서 호출)
    public CompanyListResponseDto searchCompanyWithScope(String name, UUID hubId,
                                                         UUID headerUserId, String headerUserRole) {
        String role = headerUserRole == null ? "" : headerUserRole.toUpperCase();

        UUID effectiveHubId = hubId;
        if ("HUB".equals(role)) {
            // 허브 관리자는 자신의 허브로만 제한
            try {
                HubByAdminResponse hubInfo = hubClient.getHubByAdmin(headerUserId);
                if (hubInfo == null || hubInfo.hubId() == null) {
                    // 허브 정보를 확인할 수 없으면 검색 허용하지 않음
                    return new CompanyListResponseDto(List.of());
                }
                effectiveHubId = hubInfo.hubId();
            } catch (Exception ignore) {
            }
        }
        // MASTER, COMPANY, DELIVERY: 읽기 권한은 허용 (필요 시 세분화 가능)
        return searchCompany(name, effectiveHubId);
    }

    @Transactional
    public void createCompanyWithAuth(@Valid CompanySaveRequestDto dto,
                                      UUID headerUserId, String headerUserRole) {
        String role = headerUserRole == null ? "" : headerUserRole.toUpperCase();

        if ("MASTER".equals(role)) {
            // 모두 허용
        } else if ("HUB".equals(role)) {
            // 허브 관리자는 자신의 허브에 소속된 업체만 생성 가능
            UUID adminHubId = null;
            try {
                HubByAdminResponse hubInfo = hubClient.getHubByAdmin(headerUserId);
                if (hubInfo != null) adminHubId = hubInfo.hubId();
            } catch (Exception ignore) {
            }
            if (adminHubId == null || dto.hubId() == null || !adminHubId.equals(dto.hubId())) {
                throw new com.tunaforce.company.exception.ForbiddenException("허브 관리자는 소속 허브의 업체만 생성할 수 있습니다.");
            }
        } else {
            // 업체 담당자, delivery는 생성 불가능
            throw new com.tunaforce.company.exception.ForbiddenException("생성 권한이 없습니다.");
        }

        companyRepository.save(Company.builder()
                .companyName(dto.companyName())
                .address(dto.address())
                .companyType(CompanyType.fromString(dto.type()))
                .hubId(dto.hubId())
                .userId(dto.userId())
                .build());
    }

    @Transactional
    public void editCompanyInfo(UUID companyId, @Valid CompanySaveRequestDto companySaveRequestDto,
                                UUID headerUserId, String headerUserRole) {
        Company company = companyRepository.findById(companyId).orElseThrow(CompanyNotFoundException::new);

        // 권한 정책
        // - MASTER: 모두 허용
        // - HUB: 자신의 허브 소속 업체만 수정 가능
        // - COMPANY(업체 담당자): 자신의 업체만 수정 가능
        String role = headerUserRole == null ? "" : headerUserRole.toUpperCase();
        boolean isMaster = role.equals("MASTER");
        boolean isHub = role.equals("HUB");
        boolean isCompany = role.equals("COMPANY");

        if (!isMaster) {
            if (isHub) {
                // 허브 관리자의 소속 허브를 허브 서비스에서 조회
                // 토큰 subject=사용자 ID
                UUID adminHubId = null;
                try {
                    HubByAdminResponse hubInfo = hubClient.getHubByAdmin(headerUserId);
                    if (hubInfo != null) adminHubId = hubInfo.hubId();
                } catch (Exception ignore) {
                }

                if (adminHubId == null || !adminHubId.equals(company.getHubId())) {
                    throw new com.tunaforce.company.exception.ForbiddenException("허브 관리자 권한으로는 소속 허브 내 업체만 수정 가능합니다.");
                }
            } else if (isCompany) {
                // 업체 담당자는 본인 업체만
                if (company.getUserId() == null || !company.getUserId().equals(headerUserId)) {
                    throw new com.tunaforce.company.exception.ForbiddenException("업체 담당자는 자신의 업체만 수정할 수 있습니다.");
                }
            } else {
                // 그 외 권한 없음
                throw new com.tunaforce.company.exception.ForbiddenException("수정 권한이 없습니다.");
            }
        }
        // hubId/userId는 DTO에서 바로 UUID로 전달됨

        company.updateInfo(
                companySaveRequestDto.companyName(),
                companySaveRequestDto.address(),
                CompanyType.fromString(companySaveRequestDto.type()),
                companySaveRequestDto.hubId(),
                companySaveRequestDto.userId()
        );

    }

    @Transactional
    public void deleteCompany(UUID companyId, UUID headerUserId, String headerUserRole) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(CompanyNotFoundException::new);

        // 삭제 권한 정책
        // - MASTER: 모두 허용
        // - HUB: 자신의 허브 소속 업체만 삭제 가능
        // - COMPANY: 삭제 불가
        String role = headerUserRole == null ? "" : headerUserRole.toUpperCase();
        if ("MASTER".equals(role)) {
            // ok
        } else if ("HUB".equals(role)) {
            UUID adminHubId = null;
            try {
                HubByAdminResponse hubInfo = hubClient.getHubByAdmin(headerUserId);
                if (hubInfo != null) adminHubId = hubInfo.hubId();
            } catch (Exception ignore) {
            }

            if (adminHubId == null || !adminHubId.equals(company.getHubId())) {
                throw new com.tunaforce.company.exception.ForbiddenException("허브 관리자는 소속 허브 내 업체만 삭제할 수 있습니다.");
            }
        } else {
            throw new com.tunaforce.company.exception.ForbiddenException("삭제 권한이 없습니다.");
        }

        company.delete(headerUserId);
    }

    public CompanyListResponseDto searchCompanyByIdList(CompanyIdListRequestDto requestDto) {
        List<UUID> ids = (requestDto == null) ? null : requestDto.companyIdList();
        if (ids == null || ids.isEmpty()) {
            return new CompanyListResponseDto(List.of());
        }

        List<Company> companies = companyRepository.findByCompanyIdInAndDeletedAtIsNull(ids);
        return toCompanyListResponse(companies);
    }

    public CompanyResponseDto searchCompanyByUserId(UUID userId) {
        Company company = companyRepository.findFirstByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(CompanyNotFoundException::new);

        String hubName = resolveHubName(company.getHubId());

        return new CompanyResponseDto(
                company.getCompanyId(),
                company.getCompanyName(),
                company.getCompanyType().name(),
                company.getAddress(),
                hubName
        );
    }

    // hubName 조회 중복 로직 추출: per-request 캐시를 사용해 허브 서비스 중복 호출 방지
    private String resolveHubName(UUID hubId, Map<UUID, String> cache) {
        if (hubId == null) return null;
        return cache.computeIfAbsent(hubId, hid -> {
            try {
                HubGetResponse hub = hubClient.getHub(hid);
                return hub != null ? hub.hubName() : null;
            } catch (Exception ignored) {
                return null;
            }
        });
    }

    // 단건 조회용 편의 메서드
    private String resolveHubName(UUID hubId) {
        return resolveHubName(hubId, new HashMap<>());
    }

    // 공통: Company 리스트를 DTO 리스트로 변환하며 hubName 조회에 요청 단위 캐시 적용
    private CompanyListResponseDto toCompanyListResponse(List<Company> companies) {
        Map<UUID, String> hubNameCache = new HashMap<>();
        List<CompanyResponseDto> items = companies.stream().map(c -> {
            String hubName = resolveHubName(c.getHubId(), hubNameCache);
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
