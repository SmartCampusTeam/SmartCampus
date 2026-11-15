package com.example.demo.repository;

import com.example.demo.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByRole(Utilisateur.Role role);
    List<Utilisateur> findByRoleAndGroupe(Utilisateur.Role role, String groupe);
}
