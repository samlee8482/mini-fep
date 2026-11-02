
package com.minifep.mock.tcp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MockTcpServer {

    // 기관별 TCP 서버 설정
    private final String institutionName;
    private final int port;
    private final String responseMessage;

    // 요청 전문 길이: 10바이트
    private static final int REQUEST_LENGTH = 10;

    /**
     * 가상 외부기관 TCP 서버 생성자
     *
     * @param institutionName 기관명
     * @param port TCP 수신 포트
     * @param responseMessage 응답 전문
     */
    public MockTcpServer(
            String institutionName,
            int port,
            String responseMessage) {

        this.institutionName = institutionName;
        this.port = port;
        this.responseMessage = responseMessage;
    }

    /**
     * 기관별 TCP 서버 실행
     */
    public void start() throws Exception {

        // 지정한 포트로 TCP 서버 실행
        try (ServerSocket server = new ServerSocket(port)) {

            System.out.println(
                "[" + institutionName + "] 서버 시작 - PORT: " + port
            );

            // 클라이언트 연결을 계속 대기
            while (true) {

                try (Socket socket = server.accept()) {

                    // 요청 전문 수신
                    InputStream in = socket.getInputStream();

                    byte[] request = in.readNBytes(REQUEST_LENGTH);

                    // 요청 전문 길이 검증
                    if (request.length != REQUEST_LENGTH) {
                        System.out.println(
                            "[" + institutionName + "] 전문 길이 오류"
                        );
                        continue;
                    }

                    String message = new String(
                        request,
                        StandardCharsets.UTF_8
                    );

                    System.out.println(
                        "[" + institutionName + "] 요청: " + message
                    );

                    // 기관별 응답 전문 송신
                    OutputStream out = socket.getOutputStream();

                    out.write(
                        responseMessage.getBytes(StandardCharsets.UTF_8)
                    );
                    out.flush();

                    System.out.println(
                        "[" + institutionName + "] 응답: " + responseMessage
                    );
                }
            }
        }
    }

    public static void main(String[] args) {

        // 실행 인자로 기관을 구분한다.
        // 예: java MockTcpServer A
        String institution = args.length > 0 ? args[0] : "A";

        MockTcpServer server;

        if ("A".equalsIgnoreCase(institution)) {

            server = new MockTcpServer(
                "기관 A", 9001, "SUCCESS001"
            );

        } else if ("B".equalsIgnoreCase(institution)) {

            server = new MockTcpServer(
                "기관 B", 9002, "SUCCESS002"
            );

        } else {
            System.out.println("지원하지 않는 기관입니다.");
            return;
        }

        try {
            server.start();
        } catch (Exception e) {
            System.err.println(
                "TCP 서버 오류: " + e.getMessage()
            );
        }
    }
}
