package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TriviaServiceTest {

    @Test
    void triggerDelayedNextQuestionAdvancesTheRound() throws InterruptedException {
        TriviaService service = new TriviaService();

        assertEquals(1, service.getCurrentRound());

        service.triggerDelayedNextQuestion();
        Thread.sleep(3500);

        assertEquals(2, service.getCurrentRound());
    }

    @Test
    void nextQuestionIsDifferentFromTheCurrentOne() {
        TriviaService service = new TriviaService();

        TriviaQuestion firstQuestion = service.getCurrentActiveQuestion();
        assertNotNull(firstQuestion);

        service.triggerImmediateNextQuestion();
        TriviaQuestion nextQuestion = service.getCurrentActiveQuestion();

        assertNotNull(nextQuestion);
        assertNotEquals(firstQuestion.id(), nextQuestion.id());
    }
}
