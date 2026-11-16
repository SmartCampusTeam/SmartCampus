package com.example.demo.service;

import com.example.demo.dto.Dtos.SalleView;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.Reservation.Statut;
import com.example.demo.entity.Seance;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SalleRepository;
import com.example.demo.repository.SeanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalleService {

    private final SalleRepository salles;
    private final SeanceRepository seances;
    private final ReservationRepository reservations;

    /** État de chaque salle (LIBRE / OCCUPEE / EN_ATTENTE) sur un créneau donné. */
    public List<SalleView> disponibilites(LocalDateTime debut, LocalDateTime fin) {
        if (!fin.isAfter(debut) || !debut.toLocalDate().equals(fin.toLocalDate())) {
            throw new IllegalArgumentException("Créneau invalide : même jour, fin après début");
        }
        List<Reservation> resas = reservations.findByStatutInAndDateDebutBeforeAndDateFinAfter(
                List.of(Statut.CONFIRMEE, Statut.EN_ATTENTE), fin, debut);
        List<Seance> cours = seances.findByJour(debut.getDayOfWeek()).stream()
                .filter(s -> s.getHeureDebut().isBefore(fin.toLocalTime())
                          && s.getHeureFin().isAfter(debut.toLocalTime()))
                .toList();

        return salles.findByActiveTrueOrderByEtageAscNomAsc().stream().map(s -> {
            String etat = "LIBRE";
            String detail = "Disponible sur ce créneau";
            var cour = cours.stream().filter(c -> c.getSalle().getId().equals(s.getId())).findFirst();
            var confirmee = resas.stream()
                    .filter(r -> r.getSalle().getId().equals(s.getId()) && r.getStatut() == Statut.CONFIRMEE)
                    .findFirst();
            long attente = resas.stream()
                    .filter(r -> r.getSalle().getId().equals(s.getId()) && r.getStatut() == Statut.EN_ATTENTE)
                    .count();
            if (cour.isPresent()) {
                etat = "OCCUPEE";
                detail = "Cours : " + cour.get().getMatiere() + " (" + cour.get().getGroupe() + ")";
            } else if (confirmee.isPresent()) {
                etat = "OCCUPEE";
                detail = "Réservée : " + confirmee.get().getObjet();
            } else if (attente > 0) {
                etat = "EN_ATTENTE";
                detail = attente + " demande(s) en attente sur ce créneau";
            }
            return new SalleView(s.getId(), s.getNom(), s.getBatiment().getId(), s.getBatiment().getNom(),
                    s.getEtage(), s.getType().name(), s.getCapacite(), s.getEquipements(),
                    s.getColonne(), s.getLigne(), s.getLargeur(), s.getHauteur(), etat, detail);
        }).toList();
    }
}
