package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return reponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return reponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<Map<String, String>> forbidden(SecurityException e) {
        return reponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return reponse(HttpStatus.NOT_FOUND, "Élément introuvable");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> illisible(HttpMessageNotReadableException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Requête invalide");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).findFirst().orElse("Données invalides");
        return reponse(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, String>> reponse(HttpStatus s, String message) {
        return ResponseEntity.status(s).body(Map.of("message", message == null ? "Erreur" : message));
    }
}
