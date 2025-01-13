package com.rayan.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class App implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private static final String BUCKET_NAME = "ubbzh-url-shortener-storage-lambda";
    private static final String BODY_KEY = "body";
    private static final String ORIGINAL_URL_KEY = "originalUrl";
    private static final String EXPIRATION_TIME_KEY = "expirationTime";
    private static final String CODE_KEY = "code";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> request, Context context) {
        String body = (String) request.get(BODY_KEY);
        Map<String, String> bodyData = parseRequestBody(body);

        String originalUrl = bodyData.get(ORIGINAL_URL_KEY);
        long expirationTimeInSeconds = Long.parseLong(bodyData.get(EXPIRATION_TIME_KEY));

        String shortUrlCode = generateShortUrlCode();
        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);

        saveUrlDataToS3(shortUrlCode, urlData);

        Map<String, String> response = new HashMap<>();
        response.put(CODE_KEY, shortUrlCode);

        return response;
    }


    private Map<String, String> parseRequestBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception exception) {
            throw new RuntimeException("Error parsing JSON body: " + exception.getMessage(), exception);
        }
    }

    private String generateShortUrlCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void saveUrlDataToS3(String shortUrlCode, UrlData urlData) {
        try {
            String urlDataJson = objectMapper.writeValueAsString(urlData);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(shortUrlCode + ".json")
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromString(urlDataJson));
        } catch (Exception exception) {
            throw new RuntimeException("Error saving data to S3: " + exception.getMessage(), exception);
        }
    }

}