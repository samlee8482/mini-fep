package com.minifep.core.tcp;

import com.minifep.core.error.FepError;
import com.minifep.core.error.FepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * [Chapter 6] 특정 기관의 TCP 연결을 재사용하는 제한 크기 Connection Pool.
 *
 * 1. Semaphore의 허가증 하나가 '사용 중이거나 대기 중인 소켓 한 개'를 의미한다.
 * 2. borrow(): 유휴 연결이 있으면 재사용하고, 없으면 최대 개수 내에서 새로 연결한다.
 * 3. release(): 정상 거래가 끝난 연결만 유휴 큐에 반납한다.
 * 4. invalidate(): 송수신 실패, 응답 오류, Timeout 발생 시 연결을 즉시 폐기한다.
 * 5. 여러 스레드가 동시에 접근하더라도 같은 소켓을 중복 대여하지 않는다.
 *
 * 단순 PoC용 구현이다. 유휴 만료는 대여 시 지연 정리(lazy eviction)한다.
 * 만료 전 상대방이 먼저 종료하는 경우는 여전히 존재하며, 거래 자동 재전송은 하지 않는다.
 */
public final class TcpConnectionPool implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TcpConnectionPool.class);
    private final String name;
    private final String host;
    private final int port;
    private final int connectTimeout;
    private final int readTimeout;
    private final int borrowTimeout;
    private final long maxIdleNanos;
    private final Semaphore permits;
    /** 반납된 소켓과 마지막 반납 시각을 함께 보관한다. */
    private record IdleConnection(Socket socket, long returnedAtNanos) { }
    private final Deque<IdleConnection> idle = new ArrayDeque<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TcpConnectionPool(String name, String host, int port, int connectTimeout,
                             int readTimeout, int maxConnections, int borrowTimeout, int maxIdleMs) {
        if (host == null || host.isBlank() || port < 1 || port > 65535 ||
                connectTimeout <= 0 || readTimeout <= 0 || maxConnections <= 0 || borrowTimeout <= 0 || maxIdleMs <= 0) {
            throw new IllegalArgumentException("TCP Pool 설정이 올바르지 않습니다: " + name);
        }
        this.name = name;
        this.host = host;
        this.port = port;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.borrowTimeout = borrowTimeout;
        this.maxIdleNanos = TimeUnit.MILLISECONDS.toNanos(maxIdleMs);
        this.permits = new Semaphore(maxConnections, true);
    }

    /**
     * 대여 성공 시 반드시 release 또는 invalidate를 정확히 한 번 호출해야 한다.
     * 소켓 생성 실패 시 허가증을 돌려주므로 Pool 용량이 줄어들지 않는다.
     */
    public Socket borrow() {
        if (closed.get()) throw new FepException(FepError.CONNECT_FAILURE);
        boolean acquired;
        try {
            acquired = permits.tryAcquire(borrowTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FepException(FepError.POOL_EXHAUSTED, e);
        }
        if (!acquired) throw new FepException(FepError.POOL_EXHAUSTED);
        boolean success = false;
        try {
            if (closed.get()) throw new FepException(FepError.CONNECT_FAILURE);
            // 반납 시각부터 경과한 시간이 max-idle-ms 이상이면 재사용하지 않는다.
            // System.nanoTime()은 시스템 시각 변경의 영향을 받지 않는 경과시간 측정용이다.
            // 만료 연결을 여러 개 발견하더라도 허가증은 대여 단위로 한 번만 관리한다.
            IdleConnection candidate;
            while (true) {
                synchronized (idle) { candidate = idle.pollFirst(); }
                if (candidate == null) break;
                Socket cached = candidate.socket();
                long elapsed = System.nanoTime() - candidate.returnedAtNanos();
                if (elapsed >= 0 && elapsed < maxIdleNanos
                        && cached.isConnected() && !cached.isClosed()
                        && !cached.isInputShutdown() && !cached.isOutputShutdown()) {
                    log.info("stage=TCP_POOL status=REUSED institution={}", name);
                    success = true;
                    return cached;
                }
                closeQuietly(cached);
                log.info("stage=TCP_POOL status=EXPIRED institution={}", name);
            }
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), connectTimeout);
                socket.setSoTimeout(readTimeout);
            } catch (SocketTimeoutException e) {
                closeQuietly(socket);
                throw new FepException(FepError.CONNECT_TIMEOUT, e);
            } catch (IOException e) {
                closeQuietly(socket);
                throw new FepException(FepError.CONNECT_FAILURE, e);
            }
            if (closed.get()) {
                closeQuietly(socket);
                throw new FepException(FepError.CONNECT_FAILURE);
            }
            log.info("stage=TCP_POOL status=CREATED institution={} host={} port={}", name, host, port);
            success = true;
            return socket;
        } finally {
            if (!success) permits.release();
        }
    }

    /** 정상 응답을 모두 읽은 연결만 반납한다. */
    public void release(Socket socket) {
        if (socket == null) return;
        synchronized (idle) {
            if (!closed.get() && !socket.isClosed()) {
                idle.addFirst(new IdleConnection(socket, System.nanoTime()));
                log.info("stage=TCP_POOL status=RETURNED institution={} idle={}", name, idle.size());
            } else {
                closeQuietly(socket);
            }
        }
        permits.release();
    }

    /** 오류가 발생한 소켓은 다음 거래에 전달하지 않는다. */
    public void invalidate(Socket socket) {
        closeQuietly(socket);
        log.warn("stage=TCP_POOL status=DISCARDED institution={}", name);
        permits.release();
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) try { socket.close(); } catch (IOException ignored) { }
    }

    /** 애플리케이션 종료 시 유휴 연결을 정리한다. 대여 중인 연결은 반납 시 닫힌다. */
    @Override
    public void close() {
        closed.set(true);
        synchronized (idle) {
            IdleConnection candidate;
            while ((candidate = idle.pollFirst()) != null) closeQuietly(candidate.socket());
        }
        log.info("stage=TCP_POOL status=CLOSED institution={}", name);
    }
}
