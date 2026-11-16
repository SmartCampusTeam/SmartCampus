package com.example.demo.repository;

import com.example.demo.entity.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupeRepository extends JpaRepository<Groupe, String> {
    List<Groupe> findAllByOrderByCodeAsc();
}
