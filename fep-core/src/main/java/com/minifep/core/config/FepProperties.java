
package com.minifep.core.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FEP 공통 프레임워크의 기관별 접속 설정을 관리하는 클래스.
 *
 * [역할]
 * 1. 외부 fep.yml에 정의된 기관별 접속정보를 Java 객체로 변환한다.
 * 2. 기관별 IP, PORT, 연결 타임아웃, 응답 타임아웃을 보관한다.
 * 3. 업무 애플리케이션이 기관 접속정보를 직접 관리하지 않도록 한다.
 *
 * [설정 예시]
 * fep:
 *   institutions:
 *     A:
 *       host: 127.0.0.1
 *       port: 9001
 *       connect-timeout: 3000
 *       read-timeout: 5000
 *
 * [동작 원리]
 * Spring Boot가 외부 YAML을 설정 환경에 로딩한 뒤,
 * 'fep'으로 시작하는 설정값을 이 객체에 바인딩한다.
 *
 * 실제 YAML 로딩 및 Bean 등록은 이후 설정 클래스에서 처리한다.
 */
@ConfigurationProperties(prefix = "fep")
public record FepProperties(
        Map<String, Institution> institutions) {

    /**
     * 개별 기관의 TCP 접속정보.
     *
     * @param host 기관 서버 IP 또는 호스트명
     * @param port 기관 서버 TCP 포트
     * @param connectTimeout TCP 연결 제한 시간(ms)
     * @param readTimeout TCP 응답 대기 제한 시간(ms)
     */
    public record Institution(
            String host,
            int port,
            int connectTimeout,
            int readTimeout) {
    }
}
