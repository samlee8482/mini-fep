package com.minifep.core.mapper;

/**
 * [Chapter 3 설계 및 동작 설명]
 * B 기관의 전문 구분 문자 B를 지정한다. 공통 변환 규칙은 부모 클래스를 재사용한다.
 * 기관 B 전용 전문 변환기: 기관 구분값 B를 사용한다. */
public final class InstitutionBMapper extends AbstractPrefixMessageMapper {
    public InstitutionBMapper() {
        super('B');
    }
}
