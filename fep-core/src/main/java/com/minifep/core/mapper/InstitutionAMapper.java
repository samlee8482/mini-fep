package com.minifep.core.mapper;

/**
 * [Chapter 3 설계 및 동작 설명]
 * A 기관의 전문 구분 문자 A를 지정한다. 실제 기관별 규격이 달라지면 이 클래스에서 개별화한다.
 * 기관 A 전용 전문 변환기: 기관 구분값 A를 사용한다. */
public final class InstitutionAMapper extends AbstractPrefixMessageMapper {
    public InstitutionAMapper() {
        super('A');
    }
}
