package com.github.a1k28.helper;

import com.google.gson.Gson;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@NoArgsConstructor
public class RestClient {
    private static final Gson gson = new Gson();

    private static final Logger log = Logger.getInstance(RestClient.class);


    public enum Method {
        GET, POST
    }

    public static <T> String sendRequest(
            String uri,
            Map<String, String> parameters,
            Map<String, String> headers,
            T body,
            Method method,
            Integer timeout) throws URISyntaxException, IOException, InterruptedException {
        String url = uri + getQueryString(parameters);
        log.info("Sending " + method +  " request to URL: " + url
                + ", with parameters: " + mapToString(parameters)
                + ", headers: " + mapToString(headers)
                + " & body: " + body);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(new URI(url));

        String bodyString = "";
        if (body != null) {
            if (body instanceof String) bodyString = (String) body;
            else bodyString = gson.toJson(body);
        }

        switch (method) {
            case GET -> builder.GET();
            case POST -> builder.POST(HttpRequest.BodyPublishers.ofString(bodyString));
        }

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        if (timeout != null && timeout > 0)
            builder.timeout(Duration.of(10, ChronoUnit.SECONDS));

        HttpRequest httpRequest = builder.build();

        HttpResponse<String> response = HttpClient
                .newBuilder()
                .proxy(ProxySelector.getDefault())
                .build()
                .send(httpRequest, HttpResponse.BodyHandlers.ofString());

        log.trace("Receiving response: " + response.body());
        return response.body();
    }

    private static String getQueryString(Map<String, String> map) {
        if (map == null) return "";
        StringBuilder sb = new StringBuilder();
        String prefix = "?";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(prefix);
            prefix = "&";
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static String mapToString(Map<String, String> map) {
        if (map == null) return "";
        StringBuilder sb = new StringBuilder();
        String suffix = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(suffix).append(entry.getKey()).append("=").append(entry.getValue());
            suffix = ",";
        }
        return sb.toString();
    }
}
