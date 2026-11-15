package com.example.demo.repository;

import com.example.demo.entity.Seance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface SeanceRepository extends JpaRepository<Seance, Long> {
    List<Seance> findByEnseignantId(Long enseignantId);
    List<Seance> findByJour(DayOfWeek jour);
    List<Seance> findByGroupe(String groupe);
}
