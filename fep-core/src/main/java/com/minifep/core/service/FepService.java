package com.minifep.core.service;

import com.minifep.core.config.InstitutionRegistry;
import com.minifep.core.config.InstitutionRegistry.ConnectionInfo;
import com.minifep.core.mapper.MessageMapper;
import com.minifep.core.mapper.MessageMapperRegistry;
import com.minifep.core.message.FepMessage;
import com.minifep.core.tcp.TcpClient;
import java.io.IOException;

/**
 * 업무 애플리케이션과 기관별 전문 변환/TCP 통신을 연결하는 공통 서비스.
 * Chapter 3의 처리 순서를 유지하면서 Mapper 등록소를 외부에서 주입받도록 개선했다.
 * 업무 앱은 기관별 prefix나 소켓 처리 로직을 알 필요가 없다.
 */
public class FepService {
    private final InstitutionRegistry registry;
    private final MessageMapperRegistry mapperRegistry;
    private final TcpClient tcpClient;

    /** Spring 자동 설정이 생성한 등록소를 받아 공통 서비스를 초기화한다. */
    public FepService(InstitutionRegistry registry, MessageMapperRegistry mapperRegistry) {
        this.registry = registry;
        this.mapperRegistry = mapperRegistry;
        this.tcpClient = new TcpClient();
    }

    /**
     * 1. 기관 접속정보 조회 -> 2. 기관 Mapper 조회 -> 3. 요청 본문 변환
     * -> 4. TCP 송수신(길이 헤더는 TcpClient 처리) -> 5. 응답 공통 전문 변환.
     * IOException 및 전문 검증 오류는 기존처럼 호출자에게 전달한다.
     */
    public FepMessage send(String institutionCode, FepMessage request) throws IOException {
        ConnectionInfo info = registry.get(institutionCode);
        MessageMapper mapper = mapperRegistry.get(institutionCode);
        byte[] institutionRequest = mapper.toInstitution(request);
        byte[] institutionResponse = tcpClient.send(info.host(), info.port(), institutionRequest,
                info.connectTimeout(), info.readTimeout());
        return mapper.fromInstitution(institutionResponse);
    }
}
