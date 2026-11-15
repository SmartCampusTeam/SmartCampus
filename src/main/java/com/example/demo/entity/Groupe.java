package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Groupe {

    /** Code court : GI1, GI2, GI3, MASTER, DOCT, ENS... */
    @Id
    private String code;

    private String libelle;
    private int effectif;
}
