package com.example.demo.service;

import com.example.demo.dto.Dtos.AnnonceRequest;
import com.example.demo.dto.Dtos.AnnonceView;
import com.example.demo.dto.Dtos.GroupeView;
import com.example.demo.entity.Annonce;
import com.example.demo.entity.Groupe;
import com.example.demo.entity.Notification;
import com.example.demo.entity.Utilisateur;
import com.example.demo.entity.Utilisateur.Role;
import com.example.demo.repository.AnnonceRepository;
import com.example.demo.repository.GroupeRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnonceService {

    private final GroupeRepository groupes;
    private final UtilisateurRepository users;
    private final AnnonceRepository annonces;
    private final NotificationRepository notifications;

    public List<GroupeView> groupes() {
        return groupes.findAllByOrderByCodeAsc().stream()
                .map(g -> new GroupeView(g.getCode(), g.getLibelle(), g.getEffectif(),
                        users.findByRoleAndGroupe(Role.ETUDIANT, g.getCode()).size()))
                .toList();
    }

    @Transactional
    public AnnonceView envoyer(String email, AnnonceRequest req) {
        Utilisateur auteur = users.findByEmail(email).orElseThrow();
        if (auteur.getRole() != Role.ENSEIGNANT)
            throw new SecurityException("Seuls les enseignants peuvent envoyer une annonce");
        Groupe g = groupes.findById(req.groupe())
                .orElseThrow(() -> new IllegalArgumentException("Groupe inconnu : " + req.groupe()));
        List<Utilisateur> destinataires = users.findByRoleAndGroupe(Role.ETUDIANT, g.getCode());
        if (destinataires.isEmpty())
            throw new IllegalStateException("Aucun étudiant n'est inscrit sur SmartCampus pour le groupe " + g.getCode());

        Annonce a = new Annonce();
        a.setAuteur(auteur);
        a.setGroupe(g.getCode());
        a.setMessage(req.message().trim());
        a.setNbDestinataires(destinataires.size());
        annonces.save(a);

        String texte = "Annonce de Pr. " + auteur.getNom() + " (" + g.getCode() + ") : " + a.getMessage();
        destinataires.forEach(e -> {
            Notification n = new Notification();
            n.setDestinataire(e);
            n.setMessage(texte);
            notifications.save(n);
        });
        return vue(a);
    }

    public List<AnnonceView> mesAnnonces(String email) {
        Utilisateur u = users.findByEmail(email).orElseThrow();
        return annonces.findByAuteurIdOrderByDateEnvoiDesc(u.getId()).stream().map(this::vue).toList();
    }

    private AnnonceView vue(Annonce a) {
        return new AnnonceView(a.getId(), a.getGroupe(), a.getMessage(), a.getDateEnvoi(), a.getNbDestinataires());
    }
}
