package com.minifep.server;

import com.minifep.core.message.FepMessage;
import com.minifep.core.service.FepService;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업무 앱과 FEP 코어 사이의 REST 진입점.
 * HTTP/JSON 요청을 기존 FepService의 Java 호출로 연결한다.
 * TCP 헤더 및 기관별 Mapper 처리 로직은 이곳에 중복 구현하지 않는다.
 */
@RestController
@RequestMapping("/api/fep")
public class FepController {
    private final FepService fepService;

    /** fep-core의 FepConfiguration이 등록한 FepService Bean을 주입받는다. */
    public FepController(FepService fepService) {
        this.fepService = fepService;
    }

    /**
     * POST /api/fep/send
     * 요청 예: {"institutionCode":"A","data":"HELLO00001"}
     * 응답 예: {"data":"SUCCESS001"}
     * - FepMessage는 기존대로 영문/숫자 10자리 여부를 검증한다.
     * - 미등록 기관 및 전문 형식 오류는 HTTP 400으로 전달한다.
     * - 기관 TCP 연결/읽기 오류는 HTTP 502로 전달한다.
     */
    @PostMapping("/send")
    public FepRestResponse send(@RequestBody FepRestRequest request) {
        try {
            if (request == null || request.institutionCode() == null
                    || request.institutionCode().isBlank()) {
                throw new IllegalArgumentException("기관 코드가 없습니다.");
            }
            FepMessage commonRequest = new FepMessage(request.data());
            FepMessage commonResponse = fepService.send(request.institutionCode(), commonRequest);
            return new FepRestResponse(commonResponse.data());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "기관 TCP 통신 실패", ex);
        }
    }

    /** REST 요청의 데이터 구조. HTTP 통신용이며 기관 TCP 전문과는 다르다. */
    public record FepRestRequest(String institutionCode, String data) { }

    /** REST 응답은 공통 업무 데이터만 반환한다. */
    public record FepRestResponse(String data) { }
}
