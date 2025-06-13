package org.genai.genai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    private final WebClient webClient = WebClient.builder().build();

    public IncidentAnalysisResponse callGroqStructured(String title, String description) {
        String prompt = """
                You are a cybersecurity assistant.
                Respond ONLY with a raw valid JSON object in **this exact format** — with no text before or after.
                
                {
                  "summary": "<summary>",
                  "remediationSteps": "<steps>",
                  "riskLevel": "Low|Medium|High"
                }
                
                Title: %s
                Description: %s
                """.formatted(title, description);

        Map<String, Object> requestBody = buildRequest(prompt);

        try {
            String rawResponse = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchangeToMono(response -> {
                        HttpStatusCode status = response.statusCode();

                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    if (status.is2xxSuccessful()) {
                                        return Mono.just(body);
                                    } else {
                                        String errorMessage = extractGroqError(body);
                                        return Mono.error(new GroqException(status.value(), errorMessage));
                                    }
                                });
                    })
                    .block();

            // ✅ Extract and parse LLM JSON response
            String messageContent = JsonPath.read(rawResponse, "$.choices[0].message.content");

            Pattern jsonPattern = Pattern.compile("\\{.*}", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(messageContent);

            if (matcher.find()) {
                String jsonOnly = matcher.group();
                if (!jsonOnly.contains("summary") || !jsonOnly.contains("remediationSteps")) {
                    throw new GroqException(500, "LLM response missing required fields");
                }
                // ✅ Optional cleanup: remove markdown backticks, trim
                jsonOnly = jsonOnly.replaceAll("(?i)```json", "")
                        .replaceAll("(?i)```", "")
                        .trim();

                // ✅ Auto-fix missing closing brace
                if (!jsonOnly.endsWith("}")) {
                    System.out.println("⚠️ JSON is incomplete, attempting to auto-fix...");
                    jsonOnly += "}";
                }                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonOnly, IncidentAnalysisResponse.class);
            } else {
                System.err.println("❌ Raw message content that failed:\n" + messageContent);
                throw new GroqException(500, "Could not extract JSON from LLM response: " + messageContent);
            }

        } catch (GroqException ge) {
            throw ge; // Pass it to controller
        } catch (Exception e) {
            throw new GroqException(500, "Unexpected error: " + e.getMessage());
        }
    }

    private String extractGroqError(String responseBody) {
        try {
            return JsonPath.read(responseBody, "$.error.message");
        } catch (Exception e) {
            return responseBody; // Fallback if not JSON
        }
    }

    private Map<String, Object> buildRequest(String prompt) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        // Messages list
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);

        // Final request map
        Map<String, Object> request = new HashMap<>();
        request.put("model", model); // e.g., "llama3-8b-8192"
        request.put("messages", messages);
        request.put("temperature", 0.7);

        return request;
    }

    @Getter
    public static class GroqException extends RuntimeException {
        private final int statusCode;

        public GroqException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}

/*
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    public String callOpenAI(String prompt) {
        Map<String, Object> requestBody = buildRequest(prompt);

        return webClient.post().uri(apiUrl).header("Authorization", "Bearer " + apiKey).contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody).exchangeToMono(response -> {
            HttpStatusCode status = response.statusCode();

            if (status.is2xxSuccessful()) {
                return response.bodyToMono(String.class);
            } else {
                return response.bodyToMono(String.class).flatMap(errorBody -> {
                    System.err.println("❌ OpenAI call failed with status: " + status.value());
                    System.err.println("📄 Response body: " + errorBody);

                    // Handle 429 specifically
                    if (status.value() == 429) {
                        return Mono.error(new RateLimitException("Too Many Requests (429)"));
                    }

                    return response.createException().flatMap(Mono::error);
                });
            }
        }).retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3)).filter(throwable -> throwable instanceof RateLimitException).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new RuntimeException("❌ Retried 3 times but OpenAI is still rate-limiting. Try later."))).block();
    }

    private Map<String, Object> buildRequest(String prompt) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-4o");
        request.put("messages", new Object[]{message});
        request.put("temperature", 0.7);

        return request;
    }

    // Custom exception class
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}*/

/*
package org.genai.genai;

import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    public IncidentAnalysisResponse analyzeIncident(String title, String description) {
        String prompt = "Analyze this security incident:\nTitle: " + title + "\nDescription: " + description
                + "\n\nProvide:\n1. Summary\n2. Remediation steps\n3. Risk level (Low/Medium/High)";

        Map<String, Object> request = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7
        );

        String result = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();

                    if (status.is2xxSuccessful()) {
                        return response.bodyToMono(String.class);
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("🚨 OpenAI API call failed with status: " + status.value());
                                    System.err.println("📄 Error response body: " + errorBody);

                                    if (status.value() == 429) {
                                        System.err.println("⚠️ Too Many Requests: You are being rate-limited.");
                                    }

                                    return response.createException().flatMap(Mono::error);
                                });
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("❌ WebClient error: " + ex.getStatusCode() + " - " + ex.getMessage());
                    return Mono.error(ex);
                })
                .block();


        // Extract from result JSON
        String content = JsonPath.read(result, "$.choices[0].message.content");

        // Simple parsing (or improve using regex/AI)
        IncidentAnalysisResponse response = new IncidentAnalysisResponse();
        response.summary = content.split("Remediation")[0].trim();
        response.remediationSteps = "Remediation " + content.split("Remediation")[1].split("Risk")[0].trim();
        response.riskLevel = content.contains("High") ? "High" : (content.contains("Medium") ? "Medium" : "Low");
        return response;

    }

}
*/

