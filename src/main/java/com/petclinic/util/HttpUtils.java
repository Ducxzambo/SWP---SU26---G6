package com.petclinic.util;

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

    /**
     * Thực hiện HTTP POST request
     */
    public static String post(String targetUrl, String jsonBody, Map<String, String> headers) throws Exception {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        // Gắn thêm các header tùy chỉnh
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        // Gửi request đồng bộ và nhận kết quả dưới dạng chuỗi (String)
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // Trả về chuỗi JSON từ API
        return response.body();
    }
}