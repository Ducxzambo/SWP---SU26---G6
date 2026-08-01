package com.petclinic.backend.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpUtils {

    // Tạo một Client dùng chung cho toàn bộ ứng dụng
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static String post(String targetUrl, String jsonBody, Map<String, String> headers) throws Exception {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}