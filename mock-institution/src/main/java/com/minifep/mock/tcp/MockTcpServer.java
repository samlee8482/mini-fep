package com.minifep.mock.tcp;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * [Chapter 3 설계 및 동작 설명]
 * 실제 외부기관 대신 로컬 TCP 포트 9001(A), 9002(B)에서 요청을 받는다.
 * 테스트 서버는 요청 헤더와 기관 구분자를 검증한 뒤 고정 응답을 돌려준다.
 * 한 번에 한 연결씩 처리하는 단순 PoC이며 운영용 동시 접속 서버가 아니다.
 *
 * 가상 외부기관 TCP 서버이다. 기관 A는 9001, B는 9002 포트를 사용한다.
 * 요청/응답 모두 [4바이트 ASCII 길이][11바이트 본문] 형식을 사용한다.
 */
public class MockTcpServer {
    private final String institutionName;
    private final int port;
    private final char prefix;
    private final String responseMessage;

    public MockTcpServer(String institutionName, int port, char prefix, String responseMessage) {
        this.institutionName = institutionName;
        this.port = port;
        this.prefix = prefix;
        this.responseMessage = responseMessage;
    }

    /**
     * 서버 소켓을 열고 TCP 연결을 반복 수락하여 전문을 처리한다.
     */
    public void start() throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[" + institutionName + "] 서버 시작 - PORT: " + port);
            while (true) {
                // 이번 PoC는 연결 1건을 처리하고 닫는 단순 순차 처리 서버이다.
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    InputStream in = socket.getInputStream();
                    byte[] header = in.readNBytes(4);
                    if (header.length != 4) throw new EOFException("요청 길이 헤더 부족");
                    String headerText = new String(header, StandardCharsets.US_ASCII);
                    if (!headerText.matches("[0-9]{4}")) {
                        throw new IllegalArgumentException("요청 길이 헤더 형식 오류");
                    }
                    int length = Integer.parseInt(headerText);
                    // 실습 규격은 기관 prefix 1 + 업무 데이터 10 = 총 11바이트이다.
                    if (length != 11) throw new IllegalArgumentException("요청 본문 길이 오류: " + length);
                    byte[] request = in.readNBytes(length);
                    if (request.length != length || request[0] != (byte) prefix) {
                        throw new IllegalArgumentException("요청 기관 구분 또는 전문 길이 오류");
                    }
                    String data = new String(request, 1, 10, StandardCharsets.US_ASCII);
                    System.out.println("[" + institutionName + "] 요청: " + data);

                    byte[] body = (prefix + responseMessage).getBytes(StandardCharsets.US_ASCII);
                    OutputStream out = socket.getOutputStream();
                    out.write(String.format(Locale.ROOT, "%04d", body.length)
                            .getBytes(StandardCharsets.US_ASCII));
                    out.write(body);
                    out.flush();
                    System.out.println("[" + institutionName + "] 응답: " + responseMessage);
                } catch (Exception e) {
                    // 잘못된 연결 한 건이 들어와도 가상기관 전체 서버는 유지한다.
                    System.err.println("[" + institutionName + "] 전문 처리 오류: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 프로그램의 시작점이다. 실행 인자에 따라 기관 서버 또는 업무 앱을 시작한다.
     */
    public static void main(String[] args) {
        String institution = args.length > 0 ? args[0] : "A";
        MockTcpServer server;
        if ("A".equalsIgnoreCase(institution)) {
            server = new MockTcpServer("기관 A", 9001, 'A', "SUCCESS001");
        } else if ("B".equalsIgnoreCase(institution)) {
            server = new MockTcpServer("기관 B", 9002, 'B', "SUCCESS002");
        } else {
            System.err.println("지원하지 않는 기관입니다: " + institution);
            return;
        }
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("TCP 서버 실행 오류: " + e.getMessage());
        }
    }
}
