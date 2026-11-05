package com.minifep.core.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 외부 config/fep.yml의 fep.* 설정을 Java record로 자동 변환한다.
 * Spring Boot의 relaxed binding으로 connect-timeout -> connectTimeout이 연결된다.
 * 기관별 TCP 정보와 전문 변환 규칙을 한 곳에서 관리한다.
 */
@ConfigurationProperties(prefix = "fep")
public record FepProperties(Map<String, Institution> institutions) {
    /** 기관별 접속 정보 및 Mapper 규칙. */
    public record Institution(String host, int port, int connectTimeout, int readTimeout,
                              Mapper mapper, Pool pool) { }

    /** 기관별 최대 동시 연결 수, Pool 대기시간 및 최대 유휴시간(ms). */
    public record Pool(int maxConnections, int borrowTimeout, Integer maxIdleMs) { }

    /** type=common이면 prefix를 사용하고, type=custom이면 전용 Mapper를 선택한다. */
    public record Mapper(String type, String prefix) { }
}
