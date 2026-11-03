package com.minifep.core.mapper;

import java.util.Map;

/**
 * [Chapter 3 설계 및 동작 설명]
 * 기관 코드를 기준으로 적합한 Mapper를 찾는 등록소다.
 * 현재 PoC는 A/B 두 기관만 고정 등록하며 자동 탐색 기능은 추가하지 않는다.
 * 기관 코드로 해당 기관의 Mapper를 선택한다. */
public class MessageMapperRegistry {
    // Chapter 3 범위는 가상기관 A/B 두 곳으로 제한한다.
    private final Map<String, MessageMapper> mappers = Map.of(
            "A", new InstitutionAMapper(),
            "B", new InstitutionBMapper());

    /**
     * 기관 코드를 이용해 등록된 설정 또는 Mapper를 조회한다.
     */
    public MessageMapper get(String institutionCode) {
        MessageMapper mapper = mappers.get(institutionCode);
        if (mapper == null) {
            throw new IllegalArgumentException("Mapper가 등록되지 않은 기관: " + institutionCode);
        }
        return mapper;
    }
}
