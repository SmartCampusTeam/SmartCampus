package com.example.demo.controller;

import com.example.demo.dto.Dtos.RefusRequest;
import com.example.demo.dto.Dtos.ReservationView;
import com.example.demo.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
public class AdminController {

    private final ReservationService service;

    @GetMapping
    public List<ReservationView> enAttente() {
        return service.enAttente();
    }

    @PostMapping("/{id}/confirmer")
    public ResponseEntity<Void> confirmer(@PathVariable Long id) {
        service.confirmer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refuser")
    public ResponseEntity<Void> refuser(@PathVariable Long id, @Valid @RequestBody RefusRequest body) {
        service.refuser(id, body.motif());
        return ResponseEntity.noContent().build();
    }
}
