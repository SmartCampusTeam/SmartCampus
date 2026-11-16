package com.example.demo.dto;

import com.example.demo.entity.Reservation;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ----- Profil -----
    public record ProfilView(Long id, String nom, String prenom, String email, String role,
                             String departement, String grade, String telephone,
                             String bureau, String bio, String photo, boolean notifAuto) {}

    public record ProfilUpdate(@Size(max = 30) String telephone,
                               @Size(max = 80) String departement,
                               @Size(max = 60) String grade,
                               @Size(max = 60) String bureau,
                               @Size(max = 600) String bio,
                               boolean notifAuto) {}

    public record PhotoUpdate(@NotBlank String photo) {}

    public record PasswordUpdate(@NotBlank String ancien,
                                 @NotBlank @Size(min = 6, message = "6 caractères minimum") String nouveau) {}

    // ----- Campus -----
    public record BatimentView(Long id, String code, String nom, String type, String forme,
                               double latitude, double longitude, List<double[]> polygone,
                               int nbEtages, int gridCols, int gridRows, String couleur) {}

    public record SalleView(Long id, String nom, Long batimentId, String batimentNom, int etage,
                            String type, int capacite, String equipements,
                            int colonne, int ligne, int largeur, int hauteur,
                            String etat, String detailEtat) {}

    public record SeanceView(Long id, String matiere, String groupe, String jour,
                             LocalTime heureDebut, LocalTime heureFin, String salle) {}

    // ----- Réservations -----
    public record ReservationRequest(
            @NotNull Long salleId,
            @NotNull LocalDateTime dateDebut,
            @NotNull LocalDateTime dateFin,
            @NotBlank(message = "La matière ou le thème est obligatoire") @Size(max = 120) String matiere,
            @NotBlank(message = "Le niveau est obligatoire") String niveau,
            @Min(value = 1, message = "Nombre de participants invalide") int nbParticipants,
            @NotNull Reservation.Type type,
            @NotBlank(message = "La justification est obligatoire")
            @Size(min = 20, max = 1000, message = "La justification doit contenir entre 20 et 1000 caractères") String justification,
            boolean urgent,
            boolean notifierEtudiants) {}

    public record ReservationView(Long id, Long salleId, String salle, String demandeur,
                                  LocalDateTime dateDebut, LocalDateTime dateFin, String objet,
                                  int nbParticipants, String type, String statut, String motifRefus,
                                  LocalDateTime dateDemande, int priorite, long concurrentes,
                                  String matiere, String niveau, String justification,
                                  boolean urgent, boolean notifierEtudiants) {}

    public record NotificationView(Long id, String message, LocalDateTime dateEnvoi, boolean lue) {}

    public record RefusRequest(@NotBlank(message = "Le motif est obligatoire") String motif) {}

    // ----- Étudiants et annonces -----
    public record GroupeView(String code, String libelle, int effectif, int inscrits) {}

    public record AnnonceRequest(@NotBlank(message = "Choisissez un groupe") String groupe,
                                 @NotBlank @Size(min = 5, max = 1000, message = "Le message doit contenir entre 5 et 1000 caractères") String message) {}

    public record AnnonceView(Long id, String groupe, String message, LocalDateTime dateEnvoi, int nbDestinataires) {}

    /** Séance vue par un étudiant (avec le nom de l'enseignant). */
    public record SeanceEtudiantView(Long id, String matiere, String professeur, String jour,
                                     LocalTime heureDebut, LocalTime heureFin, String salle) {}

    /** Séance exceptionnelle (rattrapage, formation…) confirmée pour le groupe de l'étudiant. */
    public record EvenementView(Long id, String objet, String matiere, String type, String salle,
                                LocalDateTime dateDebut, LocalDateTime dateFin, String professeur) {}

    // ----- Page d'accueil publique -----
    public record ApercuView(int salles, int batiments, int enseignants, int etudiants,
                             int reservationsTraitees, int enAttente, int sallesLibres, int sallesOccupees) {}
}