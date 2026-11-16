package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Seance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Utilisateur enseignant;

    private String matiere;
    private String groupe;

    @Enumerated(EnumType.STRING)
    private DayOfWeek jour;

    private LocalTime heureDebut;
    private LocalTime heureFin;

    @ManyToOne(optional = false)
    private Salle salle;
}
