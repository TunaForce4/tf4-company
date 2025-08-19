package com.tunaforce.company.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int code,
        String error,
        String message
) {
    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, "NOT_FOUND", message);
    }
}
