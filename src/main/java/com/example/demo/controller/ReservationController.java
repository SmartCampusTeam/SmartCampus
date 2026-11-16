package com.example.demo.controller;

import com.example.demo.dto.Dtos.*;
import com.example.demo.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    @GetMapping("/reservations")
    public List<ReservationView> mesReservations(Principal p) {
        return service.mesReservations(p.getName());
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationView> creer(Principal p, @Valid @RequestBody ReservationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(p.getName(), req));
    }

    @PostMapping("/reservations/{id}/annuler")
    public ResponseEntity<Void> annuler(Principal p, @PathVariable Long id) {
        service.annuler(p.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    public List<NotificationView> notifications(Principal p) {
        return service.notifications(p.getName());
    }

    @PostMapping("/notifications/lues")
    public ResponseEntity<Void> marquerLues(Principal p) {
        service.marquerLues(p.getName());
        return ResponseEntity.noContent().build();
    }
}
