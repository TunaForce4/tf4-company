package com.tunaforce.company.exception;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException() {
        super("요청하신 업체를 찾을 수 없습니다.");
    }
}
