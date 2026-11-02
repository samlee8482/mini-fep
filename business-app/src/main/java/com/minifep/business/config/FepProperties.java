
package com.minifep.business.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 fep 설정을 Java 객체로 변환한다.
 *
 * 예: fep.institutions.A.port → 9001
 */
@ConfigurationProperties(prefix = "fep")
public record FepProperties(
        Map<String, Institution> institutions) {

    /**
     * 기관별 TCP 접속 설정
     *
     * @param host 기관 IP
     * @param port 기관 포트
     * @param connectTimeout 연결 제한 시간(ms)
     * @param readTimeout 응답 제한 시간(ms)
     */
    public record Institution(
            String host,
            int port,
            int connectTimeout,
            int readTimeout) {
    }
}
