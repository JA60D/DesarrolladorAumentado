package com.example.demo;

import java.util.List;

public record TriviaQuestion(
    String id,
    String question,
    List<String> options,
    int durationInSeconds
) {}
