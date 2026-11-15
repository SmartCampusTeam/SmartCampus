package com.example.demo.controller;

import com.example.demo.dto.Dtos.EvenementView;
import com.example.demo.dto.Dtos.SeanceEtudiantView;
import com.example.demo.entity.Reservation.Statut;
import com.example.demo.entity.Utilisateur;
import com.example.demo.entity.Utilisateur.Role;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SeanceRepository;
import com.example.demo.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/** Aperçu de l'espace étudiant : emploi du temps du groupe et séances exceptionnelles annoncées. */
@RestController
@RequestMapping("/api/etudiant")
@RequiredArgsConstructor
public class EtudiantController {

    private final UtilisateurRepository users;
    private final SeanceRepository seances;
    private final ReservationRepository reservations;

    @GetMapping("/emploi-du-temps")
    public List<SeanceEtudiantView> emploiDuTemps(Principal p) {
        Utilisateur u = etudiant(p);
        return seances.findByGroupe(u.getGroupe()).stream()
                .map(s -> new SeanceEtudiantView(s.getId(), s.getMatiere(),
                        "Pr. " + s.getEnseignant().getPrenom() + " " + s.getEnseignant().getNom(),
                        s.getJour().name(), s.getHeureDebut(), s.getHeureFin(), s.getSalle().getNom()))
                .toList();
    }

    @GetMapping("/evenements")
    public List<EvenementView> evenements(Principal p) {
        Utilisateur u = etudiant(p);
        return reservations.findByNiveauAndStatutAndNotifierEtudiantsTrueAndDateFinAfterOrderByDateDebutAsc(
                        u.getGroupe(), Statut.CONFIRMEE, LocalDateTime.now()).stream()
                .map(r -> new EvenementView(r.getId(), r.getObjet(), r.getMatiere(), r.getType().name(),
                        r.getSalle().getNom(), r.getDateDebut(), r.getDateFin(),
                        "Pr. " + r.getDemandeur().getPrenom() + " " + r.getDemandeur().getNom()))
                .toList();
    }

    private Utilisateur etudiant(Principal p) {
        Utilisateur u = users.findByEmail(p.getName()).orElseThrow();
        if (u.getRole() != Role.ETUDIANT) throw new SecurityException("Espace réservé aux étudiants");
        return u;
    }
}