package com.example.demo.repository;

import com.example.demo.entity.Batiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatimentRepository extends JpaRepository<Batiment, Long> {
    List<Batiment> findAllByOrderByNomAsc();
}
