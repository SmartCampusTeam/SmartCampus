package com.example.demo.repository;

import com.example.demo.entity.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
    
    // LA MÉTHODE QUI MANQUAIT
    List<Salle> findByActiveTrueOrderByEtageAscNomAsc();
    
    Optional<Salle> findByNom(String nom);
}