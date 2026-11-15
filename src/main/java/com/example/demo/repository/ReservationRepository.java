package com.example.demo.repository;

import com.example.demo.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByDemandeurIdOrderByDateDemandeDesc(Long demandeurId);

    List<Reservation> findByStatutOrderByPrioriteDescDateDemandeAsc(Reservation.Statut statut);

    /** Réservations qui chevauchent [debut, fin] : début < fin demandée ET fin > début demandé. */
    List<Reservation> findByStatutInAndDateDebutBeforeAndDateFinAfter(
            Collection<Reservation.Statut> statuts, LocalDateTime fin, LocalDateTime debut);

    /** Séances exceptionnelles confirmées pour un groupe, que l'enseignant a choisi d'annoncer. */
    List<Reservation> findByNiveauAndStatutAndNotifierEtudiantsTrueAndDateFinAfterOrderByDateDebutAsc(
            String niveau, Reservation.Statut statut, LocalDateTime apres);
}
