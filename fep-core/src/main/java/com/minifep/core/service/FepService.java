package com.minifep.core.service;

import com.minifep.core.config.InstitutionRegistry;
import com.minifep.core.config.InstitutionRegistry.ConnectionInfo;
import com.minifep.core.mapper.MessageMapper;
import com.minifep.core.mapper.MessageMapperRegistry;
import com.minifep.core.message.FepMessage;
import com.minifep.core.tcp.TcpClient;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 기존 YAML Mapper와 TCP 송수신 호출 순서를 변경하지 않는 공통 FEP 서비스.
 * MDC의 traceId는 동일 요청 스레드에서 호출되는 TcpClient 로그에도 유지된다.
 * 전문 원문은 개인정보 노출 위험이 있어 로그에 남기지 않고 길이만 기록한다.
 */
public class FepService {
    private static final Logger log = LoggerFactory.getLogger(FepService.class);
    private final InstitutionRegistry registry;
    private final MessageMapperRegistry mapperRegistry;
    private final TcpClient tcpClient;

    public FepService(InstitutionRegistry registry, MessageMapperRegistry mapperRegistry) {
        this.registry = registry;
        this.mapperRegistry = mapperRegistry;
        this.tcpClient = new TcpClient();
    }

    public FepMessage send(String institutionCode, FepMessage request) throws IOException {
        // 기존 예외 전파 방식을 유지한다. 최종 성공/실패는 REST Controller에서 기록한다.
        log.info("stage=INSTITUTION_LOOKUP status=START institution={}", institutionCode);
        ConnectionInfo info = registry.get(institutionCode);
        MessageMapper mapper = mapperRegistry.get(institutionCode);
        log.info("stage=INSTITUTION_LOOKUP status=SUCCESS institution={}", institutionCode);

        byte[] institutionRequest = mapper.toInstitution(request);
        log.info("stage=REQUEST_MAPPING status=SUCCESS institution={} bytes={}",
                institutionCode, institutionRequest.length);

        byte[] institutionResponse = tcpClient.send(info.host(), info.port(), institutionRequest,
                info.connectTimeout(), info.readTimeout());
        log.info("stage=TCP_RESPONSE status=SUCCESS institution={} bytes={}",
                institutionCode, institutionResponse.length);

        FepMessage response = mapper.fromInstitution(institutionResponse);
        log.info("stage=RESPONSE_MAPPING status=SUCCESS institution={}", institutionCode);
        return response;
    }
}
