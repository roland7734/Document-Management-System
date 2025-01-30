package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();

    public AIService() {
    }

    public String classifyDocument(String documentContent, String existingFolders) throws IOException {
        String apiUrl = "https://api-inference.huggingface.co/models/facebook/bart-large-mnli";

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", documentContent);
        System.out.println(existingFolders);
        payload.put("parameters", Map.of(
                "candidate_labels",  Arrays.stream(existingFolders.split(","))
                        .map(str -> str.replaceAll("[\\[\\]]", "").trim())
                        .toArray(String[]::new)
        ));

        String payloadJson = objectMapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + "api_key")
                .post(RequestBody.create(payloadJson, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            List<String> labels = (List<String>) responseMap.get("labels");
            List<Double> scores = (List<Double>) responseMap.get("scores");

            String bestLabel = labels.get(0);
            return bestLabel;
        }
    }


    public String summarizeDocument(String documentContent) throws IOException {
        String apiUrl = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";

        Map<String, String> payload = new HashMap<>();
        payload.put("inputs", documentContent);

        String payloadJson = objectMapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + "api_key")
                .post(RequestBody.create(payloadJson, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            System.out.println(responseBody);
            List<Map<String, String>> responseList = objectMapper.readValue(responseBody, new TypeReference<List<Map<String, String>>>() {});
            return responseList.get(0).get("summary_text");
        }
    }
}

