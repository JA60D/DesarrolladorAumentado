package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TriviaService {

    // Récords públicos para transferencia de datos entre el servicio y el controlador
    public record QuestionSummaryDto(String questionText, String chosenAnswer, String correctAnswer, boolean wasCorrect) {}
    public record PlayerStatsDto(String username, double successRate, double errorRate, List<QuestionSummaryDto> history) {}

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> nextQuestionTask;
    private final Random random = new Random();

     private final List<String> usedQuestionIds = new CopyOnWriteArrayList<>();

    private final Map<String, List<QuestionSummaryDto>> playerHistories = new ConcurrentHashMap<>();
    
    private int currentRound = 0;
    private final int MAX_ROUNDS = 5;
    private TriviaQuestion currentActiveQuestion;
    private volatile boolean isTransitioning = false;

    // Banco de datos encapsulado en el servicio
    private final List<TriviaQuestion> triviaRepository = List.of(
        new TriviaQuestion("q1", "Which framework relies natively on Virtual Threads in version 4?", List.of("Angular", "Spring Boot", "React", "Django"), 15),
        new TriviaQuestion("q2", "Which Angular version stabilized Asynchronous Signals?", List.of("Angular 14", "Angular 18", "Angular 20", "Angular 22"), 15),
        new TriviaQuestion("q3", "What is the baseline Java version required for Spring Boot 4?", List.of("Java 8", "Java 11", "Java 17", "Java 21"), 15),
        new TriviaQuestion("q4", "Which built-in browser API is used by Angular for SSE streams?", List.of("HttpClient", "EventSource", "WebSocket", "Fetch"), 15),
        new TriviaQuestion("q5", "What Spring Boot 4 property enables high-concurrency Virtual Threads?", List.of("spring.threads.virtual.enabled", "server.threads.async", "spring.virtual.enable", "tomcat.threads.max"), 15),
        new TriviaQuestion("q6", "Which HTTP status code represents 'Teapot' in the RFC 2324 specification?", List.of("400", "404", "418", "500"), 15),
        new TriviaQuestion("q7", "What is the default port used by a PostgreSQL database server?", List.of("3306", "5432", "6379", "8080"), 15),
        new TriviaQuestion("q8", "Which command is used to create a new standalone component in modern Angular?", List.of("ng g c name", "ng new component", "ng add component", "ng generate structural"), 15),
        new TriviaQuestion("q9", "What is the core data format used to transmit records over SSE streams?", List.of("Binary", "Plain Text", "XML", "Protobuf"), 15),
        new TriviaQuestion("q10", "Which Java feature introduces immutable data structures using a simple keyword?", List.of("Class", "Interface", "Enum", "Record"), 15),
        new TriviaQuestion("q11", "What does the 'S' stand for in the famous SOLID design principles?", List.of("Static", "Single Responsibility", "State Management", "Sequential"), 15),
        new TriviaQuestion("q12", "Which tool is standard for containerizing and packaging microservices?", List.of("Docker", "Kubernetes", "Git", "Jenkins"), 15),
        new TriviaQuestion("q13", "What Angular feature allows transforming template outputs directly in the HTML layout?", List.of("Directives", "Signals", "Pipes", "Services"), 15),
        new TriviaQuestion("q14", "Which database caching engine saves data structures natively directly in RAM memory?", List.of("MySQL", "MongoDB", "Redis", "Oracle"), 15),
        new TriviaQuestion("q15", "What Spring Boot annotation is used to map incoming HTTP body JSON payloads to objects?", List.of("@RequestParam", "@PathVariable", "@RequestBody", "@ModelAttribute"), 15),
        new TriviaQuestion("q16", "Which technology is designed to orchestrate and scale thousands of Docker containers?", List.of("Docker Compose", "Kubernetes", "Ansible", "Terraform"), 15),
        new TriviaQuestion("q17", "What is the primary language used to define infrastructure blueprints in Terraform?", List.of("JSON", "YAML", "HCL", "Python"), 15),
        new TriviaQuestion("q18", "Which HTTP header is mandatory for establishing an SSE connection stream?", List.of("Content-Type: application/json", "Content-Type: text/event-stream", "Accept: text/html", "Connection: close"), 15),
        new TriviaQuestion("q19", "What keyword is used in TypeScript to define a type that can be one of multiple forms?", List.of("Union", "Interface", "Enum", "Extends"), 15),
        new TriviaQuestion("q20", "Which Java garbage collector is optimized for low-latency mass virtual thread tasks?", List.of("Serial GC", "Parallel GC", "ZGC", "CMS"), 15)
    );

    public TriviaService() {
        sendNewRandomQuestion();
    }

    public void addEmitter(SseEmitter emitter) {
        this.emitters.add(emitter);
        
        // Si entra un jugador a mitad de ronda, le mandamos la pregunta activa de inmediato
        if (currentActiveQuestion != null && !isTransitioning) {
            try {
                emitter.send(SseEmitter.event().name("question-event").data(currentActiveQuestion));
            } catch (IOException e) {
                this.emitters.remove(emitter);
            }
        }
    }

    public void removeEmitter(SseEmitter emitter) {
        this.emitters.remove(emitter);
    }

    public boolean isGameTransitioning() {
        return this.isTransitioning;
    }

    public TriviaQuestion getCurrentActiveQuestion() {
        return this.currentActiveQuestion;
    }

    public int getCurrentRound() {
        return this.currentRound;
    }

    public String getCorrectAnswerForId(String questionId) {
        return switch (questionId) {
            case "q1" -> "Spring Boot"; case "q2" -> "Angular 22"; case "q3" -> "Java 17";
            case "q4" -> "EventSource"; case "q5" -> "spring.threads.virtual.enabled";
            case "q6" -> "418"; case "q7" -> "5432"; case "q8" -> "ng g c name";
            case "q9" -> "Plain Text"; case "q10" -> "Record"; case "q11" -> "Single Responsibility";
            case "q12" -> "Docker"; case "q13" -> "Pipes"; case "q14" -> "Redis";
            case "q15" -> "@RequestBody"; case "q16" -> "Kubernetes"; case "q17" -> "HCL";
            case "q18" -> "Content-Type: text/event-stream"; case "q19" -> "Union";
            case "q20" -> "ZGC";
            default -> "";
        };
    }

    public void registerUserAnswer(String player, String chosenAnswer, String correctAnswer, boolean isCorrect) {
        playerHistories.putIfAbsent(player, new ArrayList<>());
        playerHistories.get(player).add(new QuestionSummaryDto(
            currentActiveQuestion.question(),
            chosenAnswer.isBlank() ? "[No Answer/Timeout]" : chosenAnswer,
            correctAnswer,
            isCorrect
        ));
    }

    public void forceAutomaticTimeoutLogs(String player) {
        playerHistories.putIfAbsent(player, new ArrayList<>());
        if (playerHistories.get(player).size() < currentRound) {
            String correctAnswer = getCorrectAnswerForId(currentActiveQuestion.id());
            playerHistories.get(player).add(new QuestionSummaryDto(
                currentActiveQuestion.question(), "[Timeout]", correctAnswer, false
            ));
        }
    }

    public void triggerImmediateNextQuestion() {
        if (nextQuestionTask != null) {
            nextQuestionTask.cancel(false);
        }
        this.sendNewRandomQuestion();
    }

    public void triggerDelayedNextQuestion() {
        
        if (this.isTransitioning) {
        return; 
    }
    
    // El primer jugador en llegar activa la transición global de 3 segundos
    this.isTransitioning = true;
    
    if (this.nextQuestionTask != null) {
        this.nextQuestionTask.cancel(false);
    }

        this.scheduler.schedule(() -> {
            try {
                this.sendNewRandomQuestion();
            } catch (Exception e) {
                System.err.println("Error during game loop execution: " + e.getMessage());
            } finally {
                this.isTransitioning = false; 
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void sendNewRandomQuestion() {
        currentRound++;

        if (currentRound > MAX_ROUNDS) {
            broadcastCalculatedStats();
            return; 
        }

        // Filtramos el repositorio completo para excluir TODAS las preguntas ya usadas en esta partida
        List<TriviaQuestion> availableQuestions = triviaRepository.stream()
                .filter(q -> !usedQuestionIds.contains(q.id()))
                .toList();

        // Seguro de consistencia: Si por algún motivo nos quedamos sin preguntas, reiniciamos el pool
        if (availableQuestions.isEmpty()) {
            usedQuestionIds.clear();
            availableQuestions = new ArrayList<>(triviaRepository);
        }

        // Elegimos una pregunta al azar de las opciones verdaderamente disponibles
        int randomIndex = random.nextInt(availableQuestions.size());
        currentActiveQuestion = availableQuestions.get(randomIndex);
        
        // Registramos el ID para que no vuelva a salir en las siguientes rondas
        usedQuestionIds.add(currentActiveQuestion.id());

        // Transmisión SSE con limpieza segura de memoria contra conexiones rotas
        if (!emitters.isEmpty()) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("question-event").data(currentActiveQuestion));
                } catch (IOException e) { 
                    emitter.complete(); // Cierra formalmente la conexión en el servidor (Evita Fuga de Memoria)
                    emitters.remove(emitter); 
                }
            }
        }

        if (nextQuestionTask != null) {
            nextQuestionTask.cancel(false);
        }
        nextQuestionTask = scheduler.schedule(
            this::sendNewRandomQuestion, 
            currentActiveQuestion.durationInSeconds(), 
            TimeUnit.SECONDS
        );
    }

    // 3. REEMPLAZA TU MÉTODO BROADCASTCALCULATEDSTATS
    private void broadcastCalculatedStats() {
        List<PlayerStatsDto> globalStatsReport = new ArrayList<>();

        for (Map.Entry<String, List<QuestionSummaryDto>> entry : playerHistories.entrySet()) {
            String user = entry.getKey();
            //List<QuestionSummaryDto> history = entry.getValue();

             List<QuestionSummaryDto> fullHistory = entry.getValue(); 
            List<QuestionSummaryDto> history = fullHistory.stream()
                    .limit(5)
                    .collect(Collectors.toList());
            
            long totalAnswered = history.size();
            long correctCount = history.stream().filter(QuestionSummaryDto::wasCorrect).count();
            
            double base = totalAnswered > 0 ? totalAnswered : 5.0;
            double rawSuccessRate = (correctCount / base) * 100.0;
           
            double successRate = BigDecimal.valueOf(rawSuccessRate)
            .setScale(1, RoundingMode.HALF_UP)
            .doubleValue();

            double errorRate = BigDecimal.valueOf(100.0 - successRate)
            .setScale(1, RoundingMode.HALF_UP)
            .doubleValue();

            globalStatsReport.add(new PlayerStatsDto(user, successRate, errorRate, history));
        }
        
        System.out.println("Match over! Broadcasting reports from service layer...");
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("results-event").data(globalStatsReport));
            } catch (IOException e) { 
                emitter.complete();
                emitters.remove(emitter); 
            }
        }
        
        // REINICIO DE ESTADOS: Limpiamos la lista de IDs usados para la siguiente partida de 5 rondas
        currentRound = 0;
        usedQuestionIds.clear(); // <-- ¡CRITICAL REBOOT!
        playerHistories.clear();
    }

    public synchronized void resetGameEngine() {
    // 1. Cancelar cualquier tarea programada activa para evitar hilos duplicados
    if (nextQuestionTask != null && !nextQuestionTask.isDone()) {
        nextQuestionTask.cancel(true);
    }
    
    // 2. Restablecer variables de control
    this.currentRound = 1; 
    this.isTransitioning = false;
    this.playerHistories.clear(); // Limpia los puntajes anteriores
    
    // 3. Iniciar el juego enviando la primera pregunta
    this.sendNewRandomQuestion(); 
}

}


