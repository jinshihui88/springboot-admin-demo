package com.example.adminserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Admin Server 集成测试：上下文加载 + instances API + UI 首页可达。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminServerApplicationTests {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void instancesApiShouldBeReachable() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> resp = rest.exchange(url("/instances"), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody() != null && resp.getBody().startsWith("["),
                "instances API 应返回 JSON 数组");
    }

    @Test
    void adminUiShouldBeReachable() {
        ResponseEntity<String> resp = rest.getForEntity(url("/"), String.class);
        assertTrue(resp.getStatusCode().is2xxSuccessful() || resp.getStatusCode().is3xxRedirection(),
                "UI 首页应可达，实际状态: " + resp.getStatusCode());
    }
}
