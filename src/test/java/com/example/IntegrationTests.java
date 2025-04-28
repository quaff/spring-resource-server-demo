package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTests {

    private static final String DEMO_CLIENT_ID = "client-id";
    private static final String DEMO_CLIENT_SECRET = "client-secret";

    @LocalServerPort
    private int port;

    @Test
    void withoutAccessToken() {
        RestClient restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
        assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() -> restClient.get().uri("/principal").retrieve().toBodilessEntity());
    }

    @Test
    void withAccessToken() {
        RestClient restClient = restClientForAuthorizationServer();

        // create access token
        Map<String, Object> result = restClient.post().uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(MultiValueMap.fromSingleValue(Map.of("grant_type", "client_credentials")))
                .retrieve().body(new ParameterizedTypeReference<>() {
                });
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("token_type", "Bearer");
        assertThat(result).containsKey("access_token").containsKey("expires_in");
        String accessToken = (String) result.get("access_token");

        restClient = restClientForResourceServer(accessToken);
        String body = restClient.get().uri("/principal").retrieve().toEntity(String.class).getBody();
        assertThat(body).isEqualTo(DEMO_CLIENT_ID);
    }

    private RestClient restClientForAuthorizationServer() {
        return RestClient.builder().baseUrl("http://localhost:9000")
                .requestInterceptor(new BasicAuthenticationInterceptor(DEMO_CLIENT_ID, DEMO_CLIENT_SECRET))
                .build();
    }

    private RestClient restClientForResourceServer(String accessToken) {
        return RestClient.builder().baseUrl("http://localhost:" + port).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }
}
