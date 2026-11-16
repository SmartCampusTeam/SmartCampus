package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Utilisateur {

    public enum Role { ENSEIGNANT, ETUDIANT, ETUDIANT_RESPONSABLE, DOCTORANT, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    @Column(unique = true, nullable = false)
    private String email;

    private String motDePasse;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String departement;
    private String grade;
    private String telephone;
    private boolean actif = true;

    // --- Profil enseignant ---
    private String bureau;

    @Column(length = 600)
    private String bio;

    /** Photo de profil (data URL JPEG/PNG/WebP, réduite côté navigateur). */
    @Column(columnDefinition = "TEXT")
    private String photo;

    /** Informer automatiquement mes étudiants lorsqu'un rattrapage est confirmé. */
    private boolean notifAuto = true;

    // --- Étudiants ---
    /** Code du groupe (ex. GI3) pour les comptes étudiants. */
    private String groupe;
}