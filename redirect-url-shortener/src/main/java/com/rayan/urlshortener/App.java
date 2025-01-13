package com.rayan.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class App implements RequestHandler<Map<String, Object>, Map<String, Object>> {


    private static final String BUCKET_NAME = "ubbzh-url-shortener-storage-lambda";
    private static final String ERROR_FETCHING_OBJECT = "Error fetching object from S3: ";
    private static final String ERROR_DESERIALIZING_OBJECT = "Error deserializing object data from S3: ";
    private static final int STATUS_CODE_GONE = 410;
    private static final int STATUS_CODE_FOUND = 302;

    private final S3Client s3Client = S3Client.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {

        String shortUrlCode = extractShortUrlCode(request);
        InputStream s3ObjectStream = fetchS3Object(shortUrlCode);
        UrlData urlData = deserializeUrlData(s3ObjectStream);
        return createResponse(urlData);
    }


    private String extractShortUrlCode(Map<String, Object> request) {
        String pathParameters = (String) request.get("rawPath");
        String shortUrlCode = pathParameters.replace("/", "");
        if (shortUrlCode == null || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid request: 'shortUrlCode' is required.");
        }
        return shortUrlCode;
    }


    private InputStream fetchS3Object(String shortUrlCode) {
        GetObjectRequest obj = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(shortUrlCode + ".json")
                .build();
        try {
            return s3Client.getObject(obj);
        } catch (Exception exception) {
            throw new RuntimeException(ERROR_FETCHING_OBJECT + exception.getMessage(), exception);
        }
    }


    private UrlData deserializeUrlData(InputStream s3ObjectStream) {
        try {
            return objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch (Exception exception) {
            throw new RuntimeException(ERROR_DESERIALIZING_OBJECT + exception.getMessage(), exception);
        }
    }


    private Map<String, Object> createResponse(UrlData urlData) {
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;
        Map<String, Object> response = new HashMap<>();
        if (urlData.getExpirationTime() < currentTimeInSeconds) {
            response.put("statusCode", STATUS_CODE_GONE);
            response.put("body", "This URL has expired.");
        } else {
            response.put("statusCode", STATUS_CODE_FOUND);
            response.put("headers", Map.of("Location", urlData.getOriginalUrl()));
        }
        return response;
    }
}