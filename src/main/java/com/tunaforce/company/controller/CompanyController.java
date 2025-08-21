package com.tunaforce.company.controller;

import com.tunaforce.company.dto.request.CompanyIdListRequestDto;
import com.tunaforce.company.dto.request.CompanySaveRequestDto;
import com.tunaforce.company.dto.response.CompanyListResponseDto;
import com.tunaforce.company.dto.response.CompanyResponseDto;
import com.tunaforce.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyResponseDto> getCompanyInfo(@PathVariable("companyId") String companyId) {
        return ResponseEntity.ok(companyService.getCompanyInfo(companyId));
    }

    @GetMapping
    public ResponseEntity<CompanyListResponseDto> searchCompany(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "hubId", required = false) UUID hubId) {
        return ResponseEntity.ok(companyService.searchCompany(name, hubId));
    }

    // user id로 업체 단건 조회
    @GetMapping("/users/{userId}")
    public ResponseEntity<CompanyResponseDto> getCompanyByUserId(@PathVariable("userId") UUID userId){
        return ResponseEntity.ok(companyService.searchCompanyByUserId(userId));
    }

    @PostMapping("/list")
    public ResponseEntity<CompanyListResponseDto> searchCompanyByList(@RequestBody CompanyIdListRequestDto requestDto) {
        return ResponseEntity.ok(companyService.searchCompanyByIdList(requestDto));
    }

    @PostMapping
    public ResponseEntity<Void> createCompany(@Valid @RequestBody CompanySaveRequestDto companySaveRequestDto) {
        companyService.createCompany(companySaveRequestDto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{companyId}")
    public ResponseEntity<Void> editCompanyInfo(
            @PathVariable("companyId") String companyId,
            @Valid @RequestBody CompanySaveRequestDto companySaveRequestDto,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Roles", required = false) String userRole) {
        companyService.editCompanyInfo(companyId, companySaveRequestDto, userId, userRole);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable("companyId") String companyId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Roles", required = false) String userRole) {
        companyService.deleteCompany(companyId, userId, userRole);
        return ResponseEntity.ok().build();
    }
}
