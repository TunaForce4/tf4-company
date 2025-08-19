package com.tunaforce.company.controller;

import com.tunaforce.company.dto.request.CompanySaveRequestDto;
import com.tunaforce.company.dto.response.CompanyListResponseDto;
import com.tunaforce.company.dto.response.CompanyResponseDto;
import com.tunaforce.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(value = "hubId", required = false) String hubId) {
        return ResponseEntity.ok(companyService.searchCompany(name, hubId));
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
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        companyService.editCompanyInfo(companyId, companySaveRequestDto, userId, userRole);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable("companyId") String companyId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        companyService.deleteCompany(companyId, userId, userRole);
        return ResponseEntity.ok().build();
    }
}
