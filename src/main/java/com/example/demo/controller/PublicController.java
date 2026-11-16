package com.example.demo.controller;

import com.example.demo.dto.Dtos.ApercuView;
import com.example.demo.entity.Reservation.Statut;
import com.example.demo.entity.Utilisateur.Role;
import com.example.demo.repository.BatimentRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SalleRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.SalleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Chiffres anonymes affichés sur la page d'accueil (aucune donnée personnelle). */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final SalleRepository salles;
    private final BatimentRepository batiments;
    private final UtilisateurRepository users;
    private final ReservationRepository reservations;
    private final SalleService salleService;

    @GetMapping("/apercu")
    public ApercuView apercu() {
        LocalDateTime debut = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime fin = debut.plusHours(1);
        if (!fin.toLocalDate().equals(debut.toLocalDate())) fin = debut.toLocalDate().atTime(23, 59);
        if (!fin.isAfter(debut)) debut = fin.minusMinutes(1);

        var etats = salleService.disponibilites(debut, fin);
        int libres = (int) etats.stream().filter(s -> "LIBRE".equals(s.etat())).count();
        int occupees = (int) etats.stream().filter(s -> "OCCUPEE".equals(s.etat())).count();
        var toutes = reservations.findAll();
        int traitees = (int) toutes.stream().filter(r -> r.getStatut() != Statut.EN_ATTENTE).count();
        int attente = (int) toutes.stream().filter(r -> r.getStatut() == Statut.EN_ATTENTE).count();

        return new ApercuView((int) salles.count(), (int) batiments.count(),
                users.findByRole(Role.ENSEIGNANT).size(), users.findByRole(Role.ETUDIANT).size(),
                traitees, attente, libres, occupees);
    }
}
