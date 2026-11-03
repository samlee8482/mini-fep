package com.minifep.core.config;

import com.minifep.core.service.FepService;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * [Chapter 3 설계 및 동작 설명]
 * Spring Boot 자동 설정을 통해 fep-core의 공통 객체를 업무 앱에 등록한다.
 * 외부 YAML의 기관 접속정보를 InstitutionRegistry에 담고 FepService에 전달한다.
 * FEP 공통 라이브러리가 업무 앱에 제공할 Spring 객체를 등록한다. */
@AutoConfiguration
@EnableConfigurationProperties(FepProperties.class)
public class FepConfiguration {
    @Bean
    /**
     * 기관별 접속 설정을 읽어 불변 등록소 Bean을 생성한다.
     */
    public InstitutionRegistry institutionRegistry(FepProperties properties) {
        // YAML 기관 설정을 실제 통신에서 사용하는 ConnectionInfo로 변환한다.
        Map<String, InstitutionRegistry.ConnectionInfo> configs = properties.institutions().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    FepProperties.Institution info = entry.getValue();
                    return new InstitutionRegistry.ConnectionInfo(info.host(), info.port(),
                            info.connectTimeout(), info.readTimeout());
                }));
        return new InstitutionRegistry(configs);
    }

    @Bean
    /**
     * 기관 등록소를 사용하는 FEP 공통 서비스 Bean을 생성한다.
     */
    public FepService fepService(InstitutionRegistry registry) {
        // 업무 앱은 new FepService() 대신 Spring 주입을 사용한다.
        return new FepService(registry);
    }
}
