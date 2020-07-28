package com.example.requestlogger;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestLoggerTestApplicationTests {
    @Value("${wiremock-port}")
    int wireMockPort;
    Logger logger;

    @Autowired
    TestRestTemplate testRestTemplate;

    @Autowired
    TestTransactionLoggerFactoryImpl testTransactionLoggerFactory;

    WireMockServer wireMockServer;

    @LocalServerPort
    int port;

    @BeforeAll
    void beforeAll() {
        wireMockServer = new WireMockServer(wireMockPort);
        wireMockServer.start();
    }

    @AfterAll
    void afterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        logger = testTransactionLoggerFactory.getLogger();
        reset(logger);

        wireMockServer.stubFor(get(urlEqualTo("/foo"))
                .willReturn(okJson("{\"val\": \"hello\"}")));
    }


    @Test
    void getWithoutTIDHeaderDoesNotLog() {
        ResponseEntity<Foo> responseEntity = getFooWithoutTIDHeader();


        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(new Foo("hello"));
        verify(logger, times(0))
                .info(anyString(), anyString());
    }

    @Test
    void getWithTIDHeaderLogsIncomingRequest() {
        ResponseEntity<Foo> responseEntity = getFooWithTIDHeader();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(new Foo("hello"));


        verify(logger, times(1))
                .info("Logging X-Tracing-ID: {}", "testtracingid");
    }

    @Test
    void getWithTIDHeaderAddsTIDHeaderToOutgoingRequests() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(get(urlEqualTo("/foo"))
                .withHeader("X-Tracing-ID", equalTo("testtracingid"))
                .willReturn(okJson("{\"val\": \"hello\"}")));


        ResponseEntity<Foo> responseEntity = getFooWithTIDHeader();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(new Foo("hello"));
    }

    private ResponseEntity<Foo> getFooWithTIDHeader() {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("X-Tracing-ID", "testtracingid");
        return testRestTemplate.exchange(new RequestEntity<Void>(headers, HttpMethod.GET, URI.create("http://localhost:" + port)), Foo.class);
    }

    private ResponseEntity<Foo> getFooWithoutTIDHeader() {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        return testRestTemplate.exchange(new RequestEntity<Void>(headers, HttpMethod.GET, URI.create("http://localhost:" + port)), Foo.class);
    }

    @RestController
    public static class TestController {
        final private TestClient testClient;

        TestController(TestClient testClient) {
            this.testClient = testClient;
        }

        @GetMapping("/")
        Foo getFoo() {
            return testClient.getFoo();
        }

    }

    @SpringBootApplication
    @EnableFeignClients
    @EnableDiscoveryClient
    public static class RequestLoggerTestApplication {

        public static void main(String[] args) {
            SpringApplication.run(RequestLoggerTestApplication.class, args);
        }

    }

    @FeignClient(value = "foos", url = "http://localhost:${wiremock-port}")
    public static interface TestClient {
        @RequestMapping(method = RequestMethod.GET, value = "/foo")
        Foo getFoo();
    }

    @Service
    @Primary
    @Scope(SCOPE_SINGLETON)
    public static class TestTransactionLoggerFactoryImpl implements TransactionLoggerFactory {
        private final Logger logger;

        public TestTransactionLoggerFactoryImpl() {
            logger = mock(Logger.class);
        }

        @Override
        public Logger getLogger() {
            return logger;
        }
    }
}
