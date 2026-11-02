
package com.minifep.core.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * FEP 공통 TCP 클라이언트
 *
 * 기관별 접속 정보는 외부에서 전달받는다.
 * 따라서 기관이 추가되어도 이 클래스를 수정할 필요가 없다.
 */
public class TcpClient {

    /**
     * 외부기관 TCP 전문 송수신
     *
     * @param host 기관 IP
     * @param port 기관 포트
     * @param request 요청 전문
     * @param responseLength 응답 전문 길이
     * @param connectTimeout 연결 제한 시간(ms)
     * @param readTimeout 응답 제한 시간(ms)
     * @return 응답 전문
     */
    public byte[] send(
            String host,
            int port,
            byte[] request,
            int responseLength,
            int connectTimeout,
            int readTimeout) throws IOException {

        // 전문 길이 및 타임아웃 검증
        if (responseLength <= 0
                || connectTimeout <= 0
                || readTimeout <= 0) {
            throw new IllegalArgumentException(
                "전문 길이 및 Timeout은 1 이상이어야 합니다."
            );
        }

        // TCP 연결 종료 시 소켓 자동 해제
        try (Socket socket = new Socket()) {

            // 1. 기관 TCP 서버 연결
            socket.connect(
                new InetSocketAddress(host, port),
                connectTimeout
            );

            // 2. 응답 대기 제한 시간 설정
            socket.setSoTimeout(readTimeout);

            // 3. 요청 전문 송신
            OutputStream out = socket.getOutputStream();
            out.write(request);
            out.flush();

            // 4. 응답 전문 수신
            // 현재 PoC는 10바이트 고정 길이 응답 방식
            InputStream in = socket.getInputStream();
            byte[] response = in.readNBytes(responseLength);

            // 5. 응답 전문 길이 검증
            if (response.length != responseLength) {
                throw new IOException(
                    "응답 전문 길이 불일치"
                );
            }

            return response;
        }
    }
}
