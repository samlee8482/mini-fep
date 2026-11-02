
package com.minifep.business.config;

import com.minifep.core.config.InstitutionRegistry;
import com.minifep.core.service.FepService;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 설정과 FEP 공통 모듈을 연결한다.
 */
@Configuration
public class FepConfiguration {

    /**
     * 외부 YAML 설정을 FEP 기관 Registry에 등록한다.
     */
    @Bean
    public InstitutionRegistry institutionRegistry(
            FepProperties properties) {

        Map<String, InstitutionRegistry.ConnectionInfo> configs =
            properties.institutions().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        FepProperties.Institution info = entry.getValue();

                        return new InstitutionRegistry.ConnectionInfo(
                            info.host(),
                            info.port(),
                            info.connectTimeout(),
                            info.readTimeout()
                        );
                    }
                ));

        return new InstitutionRegistry(configs);
    }

    /**
     * FEP 공통 서비스 등록
     */
    @Bean
    public FepService fepService(
            InstitutionRegistry registry) {

        return new FepService(registry);
    }
}
