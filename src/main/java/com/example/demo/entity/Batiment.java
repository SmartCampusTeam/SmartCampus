package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Batiment {

    public enum TypeBatiment { DEPARTEMENT, AMPHI }
    public enum Forme { RECT, CERCLE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;
    private String nom;

    @Enumerated(EnumType.STRING)
    private TypeBatiment type;

    @Enumerated(EnumType.STRING)
    private Forme forme = Forme.RECT;

    /** Position du repère (issue du fichier KML). */
    private double latitude;
    private double longitude;

    /** 4 coins "lat,lng;lat,lng;lat,lng;lat,lng" : haut-gauche, haut-droit, bas-droit, bas-gauche. */
    @Column(length = 400)
    private String polygone;

    private int nbEtages = 1;

    /** Grille interne servant à placer les salles dans l'emprise du bâtiment. */
    private int gridCols = 1;
    private int gridRows = 1;

    private String couleur;
}