package com.minifep.core.mapper;

import com.minifep.core.config.FepProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * [Chapter 3-1] 기관 코드 -> Mapper를 연결하는 등록소.
 * 기존의 Map.of("A", new InstitutionAMapper(), ...) 하드코딩을 제거한다.
 * 일반 기관은 YAML 설정을 읽어 CommonMessageMapper 인스턴스를 만든다.
 * 특수 기관은 customMappers에 등록한 전용 구현체를 선택할 수 있다.
 * 등록소는 초기화 이후 변경되지 않도록 Map.copyOf로 보관한다.
 */
public final class MessageMapperRegistry {
    private final Map<String, MessageMapper> mappers;

    public MessageMapperRegistry(FepProperties properties, Map<String, MessageMapper> customMappers) {
        if (properties.institutions() == null || properties.institutions().isEmpty()) {
            throw new IllegalArgumentException("기관 설정이 비어 있습니다.");
        }
        Map<String, MessageMapper> result = new HashMap<>();
        Map<String, MessageMapper> customs = customMappers == null ? Map.of() : customMappers;
        properties.institutions().forEach((code, institution) -> {
            FepProperties.Mapper config = institution.mapper();
            if (config == null || config.type() == null) {
                throw new IllegalArgumentException("Mapper 설정이 없습니다: " + code);
            }
            MessageMapper mapper;
            switch (config.type().toLowerCase(java.util.Locale.ROOT)) {
                case "common" -> mapper = new CommonMessageMapper(config.prefix());
                case "custom" -> {
                    mapper = customs.get(code);
                    if (mapper == null) {
                        throw new IllegalArgumentException("전용 Mapper가 등록되지 않았습니다: " + code);
                    }
                }
                default -> throw new IllegalArgumentException("지원하지 않는 Mapper 유형: " + code);
            }
            result.put(code, mapper);
        });
        this.mappers = Map.copyOf(result);
    }

    /** 미등록 기관은 기본 Mapper로 대체하지 않고 명시적으로 오류 처리한다. */
    public MessageMapper get(String institutionCode) {
        MessageMapper mapper = mappers.get(institutionCode);
        if (mapper == null) throw new IllegalArgumentException("Mapper가 등록되지 않은 기관: " + institutionCode);
        return mapper;
    }
}
