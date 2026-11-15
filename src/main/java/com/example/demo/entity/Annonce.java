package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Annonce {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Utilisateur auteur;

    private String groupe;

    @Column(length = 1000)
    private String message;

    private LocalDateTime dateEnvoi = LocalDateTime.now();
    private int nbDestinataires;
}
