package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Reservation {

    public enum Type { COURS_RATTRAPAGE, REUNION, ACTIVITE_PARASCOLAIRE, FORMATION, SOUTENANCE_THESE }
    public enum Statut { EN_ATTENTE, CONFIRMEE, REFUSEE, ANNULEE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Utilisateur demandeur;

    @ManyToOne(optional = false)
    private Salle salle;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    /** Intitulé généré : « Rattrapage — DevOps (GI3) ». */
    private String objet;
    private int nbParticipants;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    private Statut statut = Statut.EN_ATTENTE;

    private String motifRefus;
    private LocalDateTime dateDemande = LocalDateTime.now();

    /** Score de priorité (type + urgence) utilisé pour trier les demandes côté administration. */
    private int priorite;

    // --- Informations destinées à convaincre l'administration ---
    private String matiere;
    private String niveau;

    @Column(length = 1000)
    private String justification;

    private boolean urgent;

    /** Informer les étudiants du niveau concerné une fois la réservation confirmée. */
    private boolean notifierEtudiants;
}