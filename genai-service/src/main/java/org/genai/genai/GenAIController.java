package org.genai.genai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/genai")
public class GenAIController {

    @Autowired
    private GroqService groqService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeIncident(@RequestBody IncidentAnalysisRequest request) {
        try {
            IncidentAnalysisResponse aiResponse = groqService.callGroqStructured(
                    request.getTitle(), request.getDescription()
            );
            return ResponseEntity.ok(aiResponse);

        } catch (GroqService.GroqException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("❌ Groq error: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Internal error: " + e.getMessage());
        }
    }
}
