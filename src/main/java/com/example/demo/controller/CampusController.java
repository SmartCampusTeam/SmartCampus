package com.example.demo.controller;

import com.example.demo.dto.Dtos.BatimentView;
import com.example.demo.dto.Dtos.SalleView;
import com.example.demo.dto.Dtos.SeanceView;
import com.example.demo.repository.BatimentRepository;
import com.example.demo.repository.SeanceRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.SalleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CampusController {

    private final SalleService salleService;
    private final SeanceRepository seances;
    private final UtilisateurRepository users;
    private final BatimentRepository batimentRepo;

    @GetMapping("/batiments")
    public List<BatimentView> batiments() {
        return batimentRepo.findAllByOrderByNomAsc().stream().map(b -> new BatimentView(
                b.getId(), b.getCode(), b.getNom(), b.getType().name(), b.getForme().name(),
                b.getLatitude(), b.getLongitude(), polygone(b.getPolygone()),
                b.getNbEtages(), b.getGridCols(), b.getGridRows(), b.getCouleur())).toList();
    }

    @GetMapping("/salles/disponibilite")
    public List<SalleView> disponibilite(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return salleService.disponibilites(debut, fin);
    }

    @GetMapping("/emploi-du-temps")
    public List<SeanceView> emploiDuTemps(Principal p) {
        Long id = users.findByEmail(p.getName()).orElseThrow().getId();
        return seances.findByEnseignantId(id).stream()
                .map(s -> new SeanceView(s.getId(), s.getMatiere(), s.getGroupe(), s.getJour().name(),
                        s.getHeureDebut(), s.getHeureFin(), s.getSalle().getNom()))
                .toList();
    }

    private static List<double[]> polygone(String s) {
        return Arrays.stream(s.split(";")).map(p -> {
            String[] a = p.split(",");
            return new double[]{Double.parseDouble(a[0]), Double.parseDouble(a[1])};
        }).toList();
    }
}