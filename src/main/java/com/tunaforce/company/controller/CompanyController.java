package com.tunaforce.company.controller;

import com.tunaforce.company.dto.request.CompanySaveRequestDto;
import com.tunaforce.company.dto.response.CompanyListResponseDto;
import com.tunaforce.company.dto.response.CompanyResponseDto;
import com.tunaforce.company.service.CompanyService;
import feign.Response;
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

    //TODO: 업체 정보 수정
    @PatchMapping("/{companyId}")
    public ResponseEntity<Void> editCompanyInfo(@PathVariable("companyId") String companyId,
                                                @Valid @RequestBody CompanySaveRequestDto companySaveRequestDto) {
        companyService.editCompanyInfo(companyId, companySaveRequestDto);
        return ResponseEntity.ok().build();
    }
    // TODO: 업체 삭제
    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable("companyId") String companyId) {
        companyService.deleteCompany(companyId);
        return ResponseEntity.ok().build();
    }
}
