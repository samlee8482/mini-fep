package com.minifep.core.config;

import java.util.Map;

/**
 * [Chapter 3 설계 및 동작 설명]
 * 기관 코드를 IP, 포트, 타임아웃으로 연결하는 설정 등록소다.
 * Map.copyOf는 설정의 외부 변경을 막으며 미등록 기관 요청은 예외 처리한다.
 * 기관 코드를 IP/Port/Timeout으로 변환하는 공통 등록소이다. */
public class InstitutionRegistry {
    private final Map<String, ConnectionInfo> institutions;

    /** 기관 한 곳의 TCP 접속정보를 담는 변경 불가능한 객체이다. */
    public record ConnectionInfo(String host, int port, int connectTimeout, int readTimeout) { }

    public InstitutionRegistry(Map<String, ConnectionInfo> institutions) {
        // 복사본을 보관하여 외부 코드가 접속정보를 임의 변경하지 못하게 한다.
        this.institutions = Map.copyOf(institutions);
    }

    /**
     * 기관 코드를 이용해 등록된 설정 또는 Mapper를 조회한다.
     */
    public ConnectionInfo get(String institutionCode) {
        ConnectionInfo info = institutions.get(institutionCode);
        // 등록되지 않은 기관으로는 접속하지 않는다.
        if (info == null) {
            throw new IllegalArgumentException("등록되지 않은 기관: " + institutionCode);
        }
        return info;
    }
}
