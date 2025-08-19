package com.tunaforce.company.client.dto;

import java.util.UUID;

public record HubGetResponse(
        UUID hubId,
        String hubName,
        String hubAddress,
        Double hubLatitude,
        Double hubLongitude
) {}
