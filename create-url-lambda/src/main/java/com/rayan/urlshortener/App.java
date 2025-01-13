package com.rayan.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class App implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> request, Context context) {

        String body = (String) request.get("body");

        Map<String, String> bodyData;

        try {
            bodyData = objectMapper.readValue(body, Map.class);
        } catch (Exception exception) {
            throw new RuntimeException("Error parsing JSON body: " + exception.getMessage(), exception);
        }

        String originalUrl = bodyData.get("originalUrl");
        String expirationTime = bodyData.get("expirationTime");

        long expirationTimeInSeconds = Long.parseLong(expirationTime) * 3600;

        String shortUrlCode = UUID.randomUUID().toString().substring(0, 8);

        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);

        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlCode);

        return response;

    }
}