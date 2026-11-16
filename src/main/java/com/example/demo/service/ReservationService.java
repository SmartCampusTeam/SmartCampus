package com.example.demo.service;

import com.example.demo.dto.Dtos.*;
import com.example.demo.entity.*;
import com.example.demo.entity.Reservation.Statut;
import com.example.demo.entity.Utilisateur.Role;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final LocalTime OUVERTURE = LocalTime.of(8, 0);
    private static final LocalTime FERMETURE = LocalTime.of(20, 0);

    private final ReservationRepository reservations;
    private final SalleRepository salles;
    private final SeanceRepository seances;
    private final UtilisateurRepository users;
    private final NotificationRepository notifications;
    private final GroupeRepository groupes;

    // ---------- Règles métier partagées ----------

    public static String libelle(Reservation.Type t) {
        return switch (t) {
            case COURS_RATTRAPAGE -> "Rattrapage";
            case REUNION -> "Réunion";
            case FORMATION -> "Formation";
            case SOUTENANCE_THESE -> "Soutenance de thèse";
            case ACTIVITE_PARASCOLAIRE -> "Activité parascolaire";
        };
    }

    /** Priorité pédagogique : soutenance (4) > rattrapage (3) > formation (2) > autres (1), +1 si urgent. */
    public static int priorite(Reservation.Type t, boolean urgent) {
        int base = switch (t) {
            case SOUTENANCE_THESE -> 4;
            case COURS_RATTRAPAGE -> 3;
            case FORMATION -> 2;
            case REUNION, ACTIVITE_PARASCOLAIRE -> 1;
        };
        return base + (urgent ? 1 : 0);
    }

    // ---------- Enseignant ----------

    @Transactional
    public ReservationView creer(String email, ReservationRequest req) {
        Utilisateur u = utilisateur(email);
        Salle salle = salles.findById(req.salleId()).filter(Salle::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Salle introuvable"));
        Groupe groupe = groupes.findById(req.niveau())
                .orElseThrow(() -> new IllegalArgumentException("Niveau inconnu : " + req.niveau()));
        LocalDateTime d = req.dateDebut();
        LocalDateTime f = req.dateFin();

        if (!f.isAfter(d)) throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début");
        if (!d.toLocalDate().equals(f.toLocalDate()))
            throw new IllegalArgumentException("Une réservation doit tenir sur une seule journée");
        if (d.toLocalTime().isBefore(OUVERTURE) || f.toLocalTime().isAfter(FERMETURE))
            throw new IllegalArgumentException("Les réservations sont possibles de 08:00 à 20:00");
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY)
            throw new IllegalArgumentException("Aucune réservation le dimanche");
        long minutes = Duration.between(d, f).toMinutes();
        if (minutes < 30) throw new IllegalArgumentException("La durée minimale est de 30 minutes");
        if (minutes > 8 * 60) throw new IllegalArgumentException("La durée maximale est de 8 heures");
        if (d.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Le créneau doit être dans le futur");
        if (req.nbParticipants() > salle.getCapacite())
            throw new IllegalArgumentException("Capacité insuffisante : cette salle compte " + salle.getCapacite() + " places");

        boolean coursPrevu = seances.findByJour(d.getDayOfWeek()).stream().anyMatch(s ->
                s.getSalle().getId().equals(salle.getId())
                && s.getHeureDebut().isBefore(f.toLocalTime()) && s.getHeureFin().isAfter(d.toLocalTime()));
        if (coursPrevu || existeConfirmee(salle.getId(), d, f, null))
            throw new IllegalStateException("Cette salle est déjà occupée sur ce créneau");

        Reservation r = new Reservation();
        r.setDemandeur(u);
        r.setSalle(salle);
        r.setDateDebut(d);
        r.setDateFin(f);
        r.setMatiere(req.matiere().trim());
        r.setNiveau(groupe.getCode());
        r.setJustification(req.justification().trim());
        r.setUrgent(req.urgent());
        r.setNotifierEtudiants(req.notifierEtudiants());
        r.setNbParticipants(req.nbParticipants());
        r.setType(req.type());
        r.setObjet(libelle(req.type()) + " — " + r.getMatiere() + " (" + groupe.getCode() + ")");
        r.setStatut(Statut.EN_ATTENTE);
        r.setPriorite(priorite(req.type(), req.urgent()));
        reservations.save(r);

        notifier(u, "Votre demande pour " + salle.getNom() + " le " + d.format(F)
                + " a été transmise à l'administration.");
        String urgence = req.urgent() ? "URGENT — " : "";
        users.findByRole(Role.ADMIN).forEach(a -> notifier(a,
                urgence + "Nouvelle demande de " + u.getPrenom() + " " + u.getNom() + " : " + salle.getNom()
                        + " le " + d.format(F) + " (" + libelle(req.type()) + ", " + groupe.getCode() + ")"));
        return toView(r);
    }

    public List<ReservationView> mesReservations(String email) {
        return reservations.findByDemandeurIdOrderByDateDemandeDesc(utilisateur(email).getId())
                .stream().map(this::toView).toList();
    }

    @Transactional
    public void annuler(String email, Long id) {
        Reservation r = reservations.findById(id).orElseThrow();
        if (!r.getDemandeur().getEmail().equals(email))
            throw new SecurityException("Cette réservation ne vous appartient pas");
        if (r.getStatut() == Statut.REFUSEE || r.getStatut() == Statut.ANNULEE)
            throw new IllegalStateException("Cette réservation ne peut plus être annulée");
        boolean etaitConfirmee = r.getStatut() == Statut.CONFIRMEE;
        r.setStatut(Statut.ANNULEE);
        reservations.save(r);
        users.findByRole(Role.ADMIN).forEach(a -> notifier(a,
                r.getDemandeur().getNom() + " a annulé sa réservation de " + r.getSalle().getNom()
                        + " le " + r.getDateDebut().format(F)));
        if (etaitConfirmee && r.isNotifierEtudiants()) {
            informerEtudiants(r, "Annulation — " + r.getObjet() + " prévu le " + r.getDateDebut().format(F)
                    + " (Pr. " + r.getDemandeur().getNom() + ").");
        }
    }

    // ---------- Notifications ----------

    public List<NotificationView> notifications(String email) {
        return notifications.findByDestinataireIdOrderByDateEnvoiDesc(utilisateur(email).getId()).stream()
                .map(n -> new NotificationView(n.getId(), n.getMessage(), n.getDateEnvoi(), n.isLue())).toList();
    }

    @Transactional
    public void marquerLues(String email) {
        List<Notification> l = notifications.findByDestinataireIdOrderByDateEnvoiDesc(utilisateur(email).getId());
        l.forEach(n -> n.setLue(true));
        notifications.saveAll(l);
    }

    // ---------- Administration (partagé avec la partie de votre camarade) ----------

    public List<ReservationView> enAttente() {
        return reservations.findByStatutOrderByPrioriteDescDateDemandeAsc(Statut.EN_ATTENTE)
                .stream().map(this::toView).toList();
    }

    @Transactional
    public void confirmer(Long id) {
        Reservation r = demandeEnAttente(id);
        if (existeConfirmee(r.getSalle().getId(), r.getDateDebut(), r.getDateFin(), r.getId()))
            throw new IllegalStateException("Une autre réservation est déjà confirmée sur ce créneau : refusez cette demande");
        r.setStatut(Statut.CONFIRMEE);
        reservations.save(r);

        String suite = "";
        if (r.isNotifierEtudiants()) {
            String quand = r.getDateDebut().format(F) + " → " + r.getDateFin().toLocalTime();
            String prof = "Pr. " + r.getDemandeur().getNom();
            String msg = r.getType() == Reservation.Type.COURS_RATTRAPAGE
                    ? "Séance de rattrapage — " + r.getMatiere() + " (" + r.getNiveau() + ") : " + quand
                        + ", salle " + r.getSalle().getNom() + " (" + prof + ")."
                    : r.getObjet() + " : " + quand + ", salle " + r.getSalle().getNom() + " (" + prof + ").";
            int n = informerEtudiants(r, msg);
            if (n > 0) suite = " Vos étudiants (" + r.getNiveau() + ") ont été informés.";
        }
        notifier(r.getDemandeur(), "✔ Votre réservation de " + r.getSalle().getNom()
                + " le " + r.getDateDebut().format(F) + " est confirmée." + suite);
    }

    @Transactional
    public void refuser(Long id, String motif) {
        Reservation r = demandeEnAttente(id);
        r.setStatut(Statut.REFUSEE);
        r.setMotifRefus(motif);
        reservations.save(r);
        notifier(r.getDemandeur(), "✖ Votre réservation de " + r.getSalle().getNom()
                + " le " + r.getDateDebut().format(F) + " est refusée. Motif : " + motif);
    }

    // ---------- Utilitaires ----------

    private int informerEtudiants(Reservation r, String message) {
        List<Utilisateur> etudiants = users.findByRoleAndGroupe(Role.ETUDIANT, r.getNiveau());
        etudiants.forEach(e -> notifier(e, message));
        return etudiants.size();
    }

    private Reservation demandeEnAttente(Long id) {
        Reservation r = reservations.findById(id).orElseThrow();
        if (r.getStatut() != Statut.EN_ATTENTE) throw new IllegalStateException("Cette demande a déjà été traitée");
        return r;
    }

    private boolean existeConfirmee(Long salleId, LocalDateTime d, LocalDateTime f, Long exclureId) {
        return reservations.findByStatutInAndDateDebutBeforeAndDateFinAfter(List.of(Statut.CONFIRMEE), f, d)
                .stream().anyMatch(x -> x.getSalle().getId().equals(salleId)
                        && (exclureId == null || !x.getId().equals(exclureId)));
    }

    private void notifier(Utilisateur dest, String message) {
        Notification n = new Notification();
        n.setDestinataire(dest);
        n.setMessage(message);
        notifications.save(n);
    }

    private Utilisateur utilisateur(String email) {
        return users.findByEmail(email).orElseThrow();
    }

    private ReservationView toView(Reservation r) {
        long concurrentes = r.getStatut() != Statut.EN_ATTENTE ? 0 :
                reservations.findByStatutInAndDateDebutBeforeAndDateFinAfter(
                                List.of(Statut.EN_ATTENTE), r.getDateFin(), r.getDateDebut()).stream()
                        .filter(x -> !x.getId().equals(r.getId())
                                && x.getSalle().getId().equals(r.getSalle().getId())).count();
        return new ReservationView(r.getId(), r.getSalle().getId(), r.getSalle().getNom(),
                r.getDemandeur().getPrenom() + " " + r.getDemandeur().getNom(),
                r.getDateDebut(), r.getDateFin(), r.getObjet(), r.getNbParticipants(),
                r.getType().name(), r.getStatut().name(), r.getMotifRefus(),
                r.getDateDemande(), r.getPriorite(), concurrentes,
                r.getMatiere(), r.getNiveau(), r.getJustification(), r.isUrgent(), r.isNotifierEtudiants());
    }
}
