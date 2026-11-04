package com.minifep.core.config;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * [Chapter 3 설계 및 동작 설명]
 * Spring Bean 생성 전에 ./config/fep.yml을 Spring Environment로 읽어 들인다.
 * 설정 경로는 현재 작업 디렉터리 기준이므로 mini-fep 루트에서 실행해야 한다.
 *
 * Spring Bean이 생성되기 전에 외부 FEP 설정 파일을 로딩한다.
 * FEP 서버만 fep-core를 의존하므로 업무 앱에서는 이 로더가 실행되지 않는다.
 */
public class FepEnvironmentPostProcessor implements EnvironmentPostProcessor {
    /**
     * Spring 초기화 단계에서 외부 YAML을 설정 환경에 추가한다.
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 실행 위치(mini-fep 루트)를 기준으로 설정 파일을 찾는다.
        Resource resource = new FileSystemResource("./config/fep.yml");
        if (!resource.exists()) {
            throw new IllegalStateException("FEP 설정 파일을 찾을 수 없습니다: ./config/fep.yml");
        }
        try {
            // YAML을 Spring이 읽을 수 있는 설정값 목록으로 변환한다.
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            List<PropertySource<?>> sources = loader.load("fepExternalConfig", resource);
            // 기존 환경변수의 우선순위를 유지하도록 맨 뒤에 등록한다.
            for (PropertySource<?> source : sources) {
                environment.getPropertySources().addLast(source);
            }
        } catch (IOException e) {
            throw new IllegalStateException("FEP 설정 파일 로딩 실패: ./config/fep.yml", e);
        }
    }
}
