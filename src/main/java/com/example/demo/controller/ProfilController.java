package com.example.demo.controller;

import com.example.demo.dto.Dtos.*;
import com.example.demo.entity.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
public class ProfilController {

    private static final Pattern PHOTO = Pattern.compile("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]++$");
    private static final int PHOTO_MAX = 600_000;

    private final UtilisateurRepository users;
    private final PasswordEncoder encoder;

    @GetMapping
    public ProfilView profil(Principal p) {
        return vue(moi(p));
    }

    @PutMapping
    public ProfilView maj(Principal p, @Valid @RequestBody ProfilUpdate body) {
        Utilisateur u = moi(p);
        u.setTelephone(body.telephone());
        u.setDepartement(body.departement());
        u.setGrade(body.grade());
        u.setBureau(body.bureau());
        u.setBio(body.bio());
        u.setNotifAuto(body.notifAuto());
        return vue(users.save(u));
    }

    @PutMapping("/photo")
    public ProfilView photo(Principal p, @Valid @RequestBody PhotoUpdate body) {
        String img = body.photo();
        if (img.length() > PHOTO_MAX || !PHOTO.matcher(img).matches())
            throw new IllegalArgumentException("Image invalide (JPEG, PNG ou WebP, 400 Ko maximum)");
        Utilisateur u = moi(p);
        u.setPhoto(img);
        return vue(users.save(u));
    }

    @DeleteMapping("/photo")
    public ProfilView supprimerPhoto(Principal p) {
        Utilisateur u = moi(p);
        u.setPhoto(null);
        return vue(users.save(u));
    }

    @PutMapping("/mot-de-passe")
    public ResponseEntity<Void> motDePasse(Principal p, @Valid @RequestBody PasswordUpdate body) {
        Utilisateur u = moi(p);
        if (!encoder.matches(body.ancien(), u.getMotDePasse()))
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        u.setMotDePasse(encoder.encode(body.nouveau()));
        users.save(u);
        return ResponseEntity.noContent().build();
    }

    private Utilisateur moi(Principal p) {
        return users.findByEmail(p.getName()).orElseThrow();
    }

    private ProfilView vue(Utilisateur u) {
        return new ProfilView(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getRole().name(),
                u.getDepartement(), u.getGrade(), u.getTelephone(),
                u.getBureau(), u.getBio(), u.getPhoto(), u.isNotifAuto());
    }
}
