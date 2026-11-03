package com.minifep.core.message;

import java.nio.charset.StandardCharsets;

/**
 * [Chapter 3 설계 및 동작 설명]
 * 공통 전문은 업무가 다루는 데이터만 표현한다. TCP 헤더와 기관 구분자는 포함하지 않는다.
 * record는 생성자와 data() 접근자를 자동 제공하는 Java의 불변 데이터 표현이다.
 * 검증 실패 시 잘못된 전문이 TCP로 전송되기 전에 예외를 발생시킨다.
 *
 * 업무 시스템이 사용하는 기관 비종속 공통 전문이다.
 * 이번 PoC에서는 10바이트 영문/숫자 업무 데이터만 다룬다.
 * 기관 식별자, TCP 길이 헤더 등은 업무 데이터에 포함하지 않는다.
 */
public record FepMessage(String data) {
    public FepMessage {
        if (data == null || data.getBytes(StandardCharsets.US_ASCII).length != 10
                || !data.matches("[A-Za-z0-9]{10}")) {
            throw new IllegalArgumentException("공통 전문은 영문/숫자 10자리여야 합니다.");
        }
    }
}
