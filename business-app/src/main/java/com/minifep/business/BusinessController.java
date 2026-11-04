package com.minifep.business;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업무 요청을 받는 HTTP 진입점.
 * 업무 앱 자체는 FEP TCP 소켓이나 전문 Mapper를 직접 다루지 않는다.
 * 요청이 실제로 도착했을 때에만 FEP 서버의 REST API를 호출한다.
 *
 * 테스트: POST http://127.0.0.1:8083/api/business/send
 * JSON: {"institutionCode":"A","data":"HELLO00001"}
 */
@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private final RestClient fepClient;

    /** application.yml에 지정한 FEP 서버 URL로 REST 클라이언트를 만든다. */
    public BusinessController(RestClient.Builder builder,
            @Value("${fep.server.url}") String fepServerUrl) {
        this.fepClient = builder.baseUrl(fepServerUrl).build();
    }

    /**
     * 1. 업무 요청을 수신한다.
     * 2. 같은 기관 코드와 업무 데이터를 FEP 서버로 전달한다.
     * 3. FEP 응답을 업무 요청자에게 반환한다.
     * 4. FEP 중지/연결 실패/HTTP 오류 시 이 요청만 502로 처리한다.
     *    예외를 HTTP 응답으로 변환하므로 업무 앱 프로세스는 종료되지 않는다.
     */
    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FepRestResponse send(@RequestBody FepRestRequest request) {
        if (request == null || request.institutionCode() == null
                || request.institutionCode().isBlank() || request.data() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기관 코드 또는 업무 데이터가 없습니다.");
        }
        try {
            FepRestResponse response = fepClient.post()
                    .uri("/api/fep/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FepRestResponse.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FEP 서버 응답이 비어 있습니다.");
            }
            return response;
        } catch (RestClientException ex) {
            // HTTP 연결 실패 및 FEP 서버 4xx/5xx 오류는 현재 요청만 실패 처리한다.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FEP 서버 호출 실패", ex);
        }
    }

    /** 업무 앱과 FEP 서버 사이의 JSON 요청 규격. */
    public record FepRestRequest(String institutionCode, String data) { }

    /** FEP 서버의 JSON 응답 규격. */
    public record FepRestResponse(String data) { }
}
