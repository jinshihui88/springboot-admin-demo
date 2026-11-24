package com.example.adminclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Admin Client 集成测试。
 *
 * 测试环境禁用 SBA 注册（spring.boot.admin.client.enabled=false），
 * 避免向不存在的 Server 周期注册产生干扰日志。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.boot.admin.client.enabled=false")
class AdminClientApplicationTests {

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
    void healthEndpointShouldExposeDetails() {
        ResponseEntity<Map> resp = rest.getForEntity(url("/actuator/health"), Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map body = resp.getBody();
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("components"), "show-details=always 时应包含 components");
        assertTrue(body.get("components").toString().contains("payment-channel"),
                "自定义健康指示器应出现在 components 中");
    }

    @Test
    void loggersEndpointShouldBeExposed() {
        ResponseEntity<Map> resp = rest.getForEntity(url("/actuator/loggers"), Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void orderApiShouldWork() {
        var request = new org.springframework.http.HttpEntity<>(
                Map.of("product", "键盘", "quantity", 2));
        ResponseEntity<Map> created = rest.postForEntity(url("/api/orders"), request, Map.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());

        ResponseEntity<Map> detail = rest.getForEntity(url("/api/orders/1"), Map.class);
        assertEquals(HttpStatus.OK, detail.getStatusCode());
    }
}
