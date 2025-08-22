package com.tunaforce.company.dto.response;

import java.util.List;

public record CompanyListResponseDto(
	List<CompanyResponseDto> data
) {}
