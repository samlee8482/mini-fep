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
    private String testMode = "normal";

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
                // Chapter 6: 연결별 가상 스레드로 처리하여 여러 Pool 연결을 동시에 수락한다.
                // 동일 소켓에서 요청 전문을 반복 읽으므로 다음 거래에서도 연결이 유지된다.
                Socket accepted = server.accept();
                Thread.ofVirtual().start(() -> handleConnection(accepted));
            }
        }
    }

    /**
     * 한 연결에서 여러 건의 요청을 순서대로 처리한다.
     * EOF는 클라이언트가 정상적으로 소켓을 닫았다는 의미다.
     * 요청이 없는 유휴 연결은 무한히 기다리지 않도록 30초 후 정리한다.
     */
    private void handleConnection(Socket accepted) {
        try (Socket socket = accepted) {
            socket.setSoTimeout(30000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            System.out.println("[" + institutionName + "] TCP 연결 수락: " + socket.getRemoteSocketAddress());
            while (true) {
                byte[] header = in.readNBytes(4);
                if (header.length == 0) return; // 상대방 정상 종료
                if (header.length != 4) throw new EOFException("요청 길이 헤더 부족");
                String headerText = new String(header, StandardCharsets.US_ASCII);
                if (!headerText.matches("[0-9]{4}")) {
                    throw new IllegalArgumentException("요청 길이 헤더 형식 오류");
                }
                int length = Integer.parseInt(headerText);
                if (length != 11) throw new IllegalArgumentException("요청 본문 길이 오류: " + length);
                byte[] request = in.readNBytes(length);
                if (request.length != length || request[0] != (byte) prefix) {
                    throw new IllegalArgumentException("요청 기관 구분 또는 전문 길이 오류");
                }
                String data = new String(request, 1, 10, StandardCharsets.US_ASCII);
                System.out.println("[" + institutionName + "] 요청: " + data);
                // Chapter 5의 장애 재현 모드는 그대로 유지한다.
                if ("delay".equals(testMode)) Thread.sleep(6000);
                byte[] body = (("wrong-prefix".equals(testMode) ? 'Z' : prefix) + responseMessage)
                        .getBytes(StandardCharsets.US_ASCII);
                out.write(("malformed".equals(testMode) ? "ABCD" : String.format(Locale.ROOT, "%04d", body.length))
                        .getBytes(StandardCharsets.US_ASCII));
                out.write(body);
                out.flush();
                System.out.println("[" + institutionName + "] 응답: " + responseMessage);
            }
        } catch (Exception e) {
            System.err.println("[" + institutionName + "] 연결 처리 종료: " + e.getMessage());
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
        // 사용 예: java ... MockTcpServer B delay
        server.testMode = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "normal";
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("TCP 서버 실행 오류: " + e.getMessage());
        }
    }
}
