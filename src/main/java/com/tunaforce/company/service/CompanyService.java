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

    public CompanyListResponseDto searchCompany(String name, UUID hubId) {
        // 빈 문자열은 null로 정규화 -> 전체 검색 동작 보장
        String normalizedName = (name == null || name.isBlank()) ? null : name;

        List<Company> companies = companyRepository.search(normalizedName, hubId);

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
        companyRepository.save(Company.builder()
                .companyName(companySaveRequestDto.companyName())
                .address(companySaveRequestDto.address())
                .companyType(CompanyType.fromString(companySaveRequestDto.type()))
                .hubId(companySaveRequestDto.hubId())
                .userId(companySaveRequestDto.userId())
                .build());
    }

    @Transactional
    public void editCompanyInfo(String companyId, @Valid CompanySaveRequestDto companySaveRequestDto,
                                UUID headerUserId, String headerUserRole) {
        Company company = companyRepository.findById(UUID.fromString(companyId)).orElseThrow(CompanyNotFoundException::new);

        // 권한: MASTER/HUB 이거나 소유자(userId 일치)
        boolean isPrivileged = headerUserRole != null && (
                headerUserRole.equalsIgnoreCase("MASTER") || headerUserRole.equalsIgnoreCase("HUB")
        );
        boolean isOwner = company.getUserId() != null && company.getUserId().equals(headerUserId);
        if (!(isPrivileged || isOwner)) {
            throw new com.tunaforce.company.exception.ForbiddenException("수정 권한이 없습니다.");
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
    public void deleteCompany(String companyId, UUID headerUserId, String headerUserRole) {
        Company company = companyRepository.findById(UUID.fromString(companyId))
                .orElseThrow(CompanyNotFoundException::new);

        // 간단한 권한 체크: ADMIN/HUB는 모두 허용, 아니면 company.userId와 일치해야 함
        boolean isPrivileged = headerUserRole != null && (
                headerUserRole.equalsIgnoreCase("ADMIN") || headerUserRole.equalsIgnoreCase("HUB")
        );
        boolean isOwner = company.getUserId() != null && company.getUserId().equals(headerUserId);

        if (!(isPrivileged || isOwner)) {
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

    public CompanyResponseDto searchCompanyByUserId(UUID userId) {
        Company company = companyRepository.findFirstByUserIdAndDeletedAtIsNull(userId)
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
