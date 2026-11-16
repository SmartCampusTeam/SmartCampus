package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
public class Salle {

    public enum TypeSalle { AMPHI, SALLE_COURS, LABO, SOUTENANCE, REUNION }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nom;

    @ManyToOne(optional = false)
    private Batiment batiment;

    private int etage;

    @Enumerated(EnumType.STRING)
    private TypeSalle type;

    private int capacite;
    private String equipements;
    
    // LE CHAMP QUI MANQUAIT
    private boolean active = true;

    // Position dans la grille du bâtiment (colonne, ligne, cases en largeur et en hauteur)
    private int colonne;
    private int ligne;
    private int largeur = 1;
    private int hauteur = 1;

    // Constructeur vide obligatoire pour JPA
    public Salle() {}

    // --- GETTERS ET SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Batiment getBatiment() { return batiment; }
    public void setBatiment(Batiment batiment) { this.batiment = batiment; }

    public int getEtage() { return etage; }
    public void setEtage(int etage) { this.etage = etage; }

    public TypeSalle getType() { return type; }
    public void setType(TypeSalle type) { this.type = type; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getEquipements() { return equipements; }
    public void setEquipements(String equipements) { this.equipements = equipements; }

    // LE GETTER QUI MANQUAIT
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getColonne() { return colonne; }
    public void setColonne(int colonne) { this.colonne = colonne; }

    public int getLigne() { return ligne; }
    public void setLigne(int ligne) { this.ligne = ligne; }

    public int getLargeur() { return largeur; }
    public void setLargeur(int largeur) { this.largeur = largeur; }

    public int getHauteur() { return hauteur; }
    public void setHauteur(int hauteur) { this.hauteur = hauteur; }
}