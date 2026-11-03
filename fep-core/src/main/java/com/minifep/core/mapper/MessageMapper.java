package com.minifep.core.mapper;

import com.minifep.core.message.FepMessage;

/**
 * [Chapter 3 설계 및 동작 설명]
 * 인터페이스는 기관별 Mapper가 반드시 제공해야 할 변환 메서드의 규약이다.
 * 요청 변환과 응답 역변환을 분리하여 업무 로직이 기관 포맷을 알 필요가 없게 한다.
 * 기관별 전문 변환기의 공통 계약이다. */
public interface MessageMapper {
    /** 공통 요청을 기관이 이해하는 본문 바이트로 변환한다. */
    byte[] toInstitution(FepMessage message);

    /** 기관 응답 본문을 공통 응답으로 변환하고 기관 구분값도 검증한다. */
    FepMessage fromInstitution(byte[] response);
}
