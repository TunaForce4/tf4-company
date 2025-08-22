package com.tunaforce.company.client;

import com.tunaforce.company.client.dto.HubGetResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// name: Eureka 서비스명 (properties로 override 가능), url: 로컬 테스트 등 직접 URL 지정시 사용
@FeignClient(name = "${clients.hub.name:hub}")
public interface HubClient {

    @GetMapping("/hubs/{hubId}")
    HubGetResponse getHub(@PathVariable("hubId") UUID hubId);

    // 허브 관리자 → 허브 조회 API 연동
    @GetMapping("/hubs/admins/{hubAdminId}")
    com.tunaforce.company.client.dto.HubByAdminResponse getHubByAdmin(@PathVariable("hubAdminId") UUID hubAdminId);
}
