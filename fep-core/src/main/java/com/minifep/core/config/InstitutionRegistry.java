
package com.minifep.core.config;

import java.util.Map;
import java.util.HashMap;

/**
 * FEP 기관별 접속 정보 관리 클래스
 *
 * 업무 애플리케이션은 기관 코드만 전달하고,
 * 실제 IP, Port, Timeout은 FEP에서 관리한다.
 */
public class InstitutionRegistry {

    // 기관별 접속 정보를 저장하는 Map
    private final Map<String, ConnectionInfo> institutions;

    /**
     * 기관별 TCP 접속 정보
     */
    public record ConnectionInfo(
            String host,
            int port,
            int connectTimeout,
            int readTimeout) {
    }

    /**
     * 외부에서 전달받은 기관 설정으로 Registry 생성
     */
    public InstitutionRegistry(
            Map<String, ConnectionInfo> institutions) {

        // 외부에서 설정 Map을 변경하지 못하도록 복사
        this.institutions = Map.copyOf(institutions);
    }

    /**
     * 기관 코드로 접속 정보 조회
     *
     * @param institutionCode 기관 코드
     * @return 기관별 TCP 접속 정보
     */
    public ConnectionInfo get(String institutionCode) {

        ConnectionInfo info = institutions.get(institutionCode);

        // 등록되지 않은 기관은 통신 차단
        if (info == null) {
            throw new IllegalArgumentException(
                "등록되지 않은 기관: " + institutionCode
            );
        }

        return info;
    }
}
