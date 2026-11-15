package com.example.demo.config;

import com.example.demo.entity.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository repo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur u = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return User.withUsername(u.getEmail())
                .password(u.getMotDePasse())
                .roles(u.getRole().name())
                .disabled(!u.isActif())
                .build();
    }
}