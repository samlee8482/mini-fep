package com.minifep.core.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * [Chapter 3 설계 및 동작 설명]
 * TCP는 메시지 경계가 없는 바이트 스트림이므로 길이 헤더로 전문 경계를 구분한다.
 * 4바이트 ASCII 숫자 헤더는 뒤따르는 본문 바이트 수를 나타낸다.
 * 연결 타임아웃은 접속 단계, 읽기 타임아웃은 응답 수신 대기 단계에 적용된다.
 * readNBytes는 지정 길이까지 읽거나 연결이 종료될 때까지 기다린다.
 *
 * 기관에 공통으로 사용하는 TCP 송수신 모듈이다.
 * 프로토콜: [ASCII 길이 헤더 4바이트][본문 N바이트].
 * 길이 헤더는 본문 길이만 나타내며 헤더 자체 4바이트는 제외한다.
 */
public class TcpClient {
    private static final Logger log = LoggerFactory.getLogger(TcpClient.class);
    private static final int HEADER_LENGTH = 4;
    private static final int MAX_BODY_LENGTH = 9999;

    /**
     * 기관별 요청을 처리하고 응답을 반환한다. 입력값 및 네트워크 오류는 호출자에게 전달한다.
     */
    public byte[] send(String host, int port, byte[] request,
                       int connectTimeout, int readTimeout) throws IOException {
        // 무한 대기나 허용 범위 초과 전문을 사전에 차단한다.
        if (request == null || request.length == 0 || request.length > MAX_BODY_LENGTH
                || connectTimeout <= 0 || readTimeout <= 0) {
            throw new IllegalArgumentException("전문 길이 또는 Timeout 설정이 올바르지 않습니다.");
        }
        // try-with-resources를 사용하면 정상/오류 모두 소켓이 자동 해제된다.
        long tcpStarted = System.nanoTime();
        log.info("stage=TCP_CONNECT status=START host={} port={}", host, port);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setSoTimeout(readTimeout);
            log.info("stage=TCP_CONNECT status=SUCCESS host={} port={}", host, port);
            OutputStream out = socket.getOutputStream();
            // 11바이트 본문이라면 '0011'을 먼저 보내고 본문을 이어서 전송한다.
            out.write(String.format(java.util.Locale.ROOT, "%04d", request.length)
                    .getBytes(StandardCharsets.US_ASCII));
            out.write(request);
            out.flush();
            log.info("stage=TCP_SEND status=SUCCESS bytes={}", request.length);

            InputStream in = socket.getInputStream();
            byte[] header = in.readNBytes(HEADER_LENGTH);
            if (header.length != HEADER_LENGTH) {
                throw new EOFException("응답 길이 헤더 수신 실패");
            }
            // 숫자 이외의 헤더를 허용하지 않는다.
            String lengthText = new String(header, StandardCharsets.US_ASCII);
            if (!lengthText.matches("[0-9]{4}")) {
                throw new IOException("응답 길이 헤더 형식 오류: " + lengthText);
            }
            int responseLength = Integer.parseInt(lengthText);
            if (responseLength <= 0 || responseLength > MAX_BODY_LENGTH) {
                throw new IOException("응답 본문 길이 오류: " + responseLength);
            }
            // TCP는 한 번의 read로 전체 전문이 오지 않을 수 있으므로 정확히 N바이트를 읽는다.
            byte[] response = in.readNBytes(responseLength);
            if (response.length != responseLength) {
                throw new EOFException("응답 본문 길이 불일치: " + response.length + "/" + responseLength);
            }
            log.info("stage=TCP_RECEIVE status=SUCCESS bytes={} elapsedMs={}",
                    response.length, (System.nanoTime() - tcpStarted) / 1_000_000);
            return response;
        } catch (IOException | RuntimeException ex) {
            log.error("stage=TCP_FINISHED status=ERROR host={} port={} errorType={} elapsedMs={}",
                    host, port, ex.getClass().getSimpleName(),
                    (System.nanoTime() - tcpStarted) / 1_000_000, ex);
            throw ex;
        }
    }
}
