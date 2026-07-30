package com.example.demo;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/trivia")
// Allows your Angular frontend (running on port 4200) to bypass security blocks
@CrossOrigin(origins = "http://localhost:4200")
public class TriviaController {

    // Modern DTOs mapped to JSON structures natively in Spring Boot 4
    public record AnswerSubmission(String username, String questionId, String chosenAnswer) {}
    
    public record ValidationResponse(boolean correct, String message) {}

    // Thread-safe list holding all open client streams
    private final TriviaService triviaService;

    public TriviaController(TriviaService triviaService) {
        this.triviaService = triviaService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQuestions() {
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(5));
        this.triviaService.addEmitter(emitter);

        emitter.onCompletion(() -> this.triviaService.removeEmitter(emitter));
        emitter.onTimeout(() -> this.triviaService.removeEmitter(emitter));
        emitter.onError((e) -> this.triviaService.removeEmitter(emitter));

        return emitter;
    }

    /**
     * 2. HTTP POST VALIDATION ENDPOINT
     * Angular submits user answer payloads here to check if they score a point.
     */
    @PostMapping("/submit")
    public ValidationResponse checkAnswer(@RequestBody AnswerSubmission submission) {
        
        TriviaQuestion currentQuestion = triviaService.getCurrentActiveQuestion();
        
        if (currentQuestion == null || !currentQuestion.id().equals(submission.questionId())) {
            return new ValidationResponse(false, "Too late! The question has already changed.");
        }

        String player = (submission.username() == null || submission.username().isBlank()) ? "Anonymous" : submission.username();
        String correctAnswer = triviaService.getCorrectAnswerForId(currentQuestion.id());
        boolean isCorrect = correctAnswer.equalsIgnoreCase(submission.chosenAnswer());

        // Ordenamos al servicio guardar el registro histórico
        triviaService.registerUserAnswer(player, submission.chosenAnswer(), correctAnswer, isCorrect);

        if (isCorrect) {
            triviaService.triggerDelayedNextQuestion();
            return new ValidationResponse(true, "Great job! That's correct.");
        } else {
            return new ValidationResponse(false, "Wrong option! Correct answer was: " + correctAnswer);
        }
    }
    /**
     * 3. ENDPOINT DE TIEMPO AGOTADO (HTTP POST)
     * Angular llama aquí cuando su reloj local llega a 0 para forzar el cambio
     * inmediato.
     */
    @PostMapping("/timeout")
    public synchronized ValidationResponse handleTimeout(@RequestBody AnswerSubmission submission) {
          TriviaQuestion currentQuestion = triviaService.getCurrentActiveQuestion();
        
        if (currentQuestion != null && currentQuestion.id().equals(submission.questionId())) {
            String player = (submission.username() == null || submission.username().isBlank()) ? "Anonymous" : submission.username();
            
            // Delegamos las penalizaciones e hilos al servicio
            triviaService.forceAutomaticTimeoutLogs(player);
            triviaService.triggerImmediateNextQuestion();

            return new ValidationResponse(true, "Timeout processed successfully.");
        }
        return new ValidationResponse(false, "Timeout ignored.");       
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetMatch() {
    triviaService.resetGameEngine(); // Método que limpia el estado
    return ResponseEntity.ok(Map.of("message", "Match reset successfully!"));
}

}
