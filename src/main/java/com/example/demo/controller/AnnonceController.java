package com.example.demo.controller;

import com.example.demo.dto.Dtos.AnnonceRequest;
import com.example.demo.dto.Dtos.AnnonceView;
import com.example.demo.dto.Dtos.GroupeView;
import com.example.demo.service.AnnonceService;
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
public class AnnonceController {

    private final AnnonceService service;

    @GetMapping("/groupes")
    public List<GroupeView> groupes() {
        return service.groupes();
    }

    @GetMapping("/annonces")
    public List<AnnonceView> mesAnnonces(Principal p) {
        return service.mesAnnonces(p.getName());
    }

    @PostMapping("/annonces")
    public ResponseEntity<AnnonceView> envoyer(Principal p, @Valid @RequestBody AnnonceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.envoyer(p.getName(), req));
    }
}
