package com.minifep.core.config;

import com.minifep.core.tcp.TcpConnectionPool;
import java.util.HashMap;
import java.util.Map;

/**
 * [Chapter 6] 기관 코드별 Connection Pool 등록소.
 * A(9001)와 B(9002)는 서로 다른 Pool을 가지므로 연결이 섞이지 않는다.
 * Spring Bean 종료 시 모든 기관의 유휴 연결을 닫는다.
 */
public final class TcpPoolRegistry implements AutoCloseable {
    private final Map<String, TcpConnectionPool> pools;

    public TcpPoolRegistry(FepProperties properties) {
        if (properties.institutions() == null || properties.institutions().isEmpty()) {
            throw new IllegalArgumentException("fep.institutions 설정이 없습니다.");
        }
        Map<String, TcpConnectionPool> created = new HashMap<>();
        try {
            properties.institutions().forEach((code, info) -> {
                FepProperties.Pool pool = info.pool();
                // 이전 YAML과의 호환성을 위해 Pool 설정 생략 시 기본값을 사용한다.
                int max = pool == null ? 5 : pool.maxConnections();
                int wait = pool == null ? 2000 : pool.borrowTimeout();
                // 기존 YAML에 max-idle-ms가 없어도 기본 20초를 적용한다.
                int idleMs = pool == null || pool.maxIdleMs() == null ? 20000 : pool.maxIdleMs();
                created.put(code, new TcpConnectionPool(code, info.host(), info.port(),
                        info.connectTimeout(), info.readTimeout(), max, wait, idleMs));
            });
        } catch (RuntimeException e) {
            created.values().forEach(TcpConnectionPool::close);
            throw e;
        }
        this.pools = Map.copyOf(created);
    }

    public TcpConnectionPool get(String institutionCode) {
        TcpConnectionPool pool = pools.get(institutionCode);
        if (pool == null) throw new IllegalArgumentException("등록되지 않은 기관: " + institutionCode);
        return pool;
    }

    @Override
    public void close() { pools.values().forEach(TcpConnectionPool::close); }
}
