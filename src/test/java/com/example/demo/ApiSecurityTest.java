package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:securitydb")
@AutoConfigureMockMvc
class ApiSecurityTest {

    @Autowired MockMvc mvc;

    @Test
    void apiProtegeeSansConnexion() throws Exception {
        mvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "prof@ensa.ma", roles = "ENSEIGNANT")
    void enseignantVoitSesReservations() throws Exception {
        mvc.perform(get("/api/reservations")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "prof@ensa.ma", roles = "ENSEIGNANT")
    void enseignantNAccedePasALAdministration() throws Exception {
        mvc.perform(get("/api/admin/reservations")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@ensa.ma", roles = "ADMIN")
    void administrateurVoitLesDemandes() throws Exception {
        mvc.perform(get("/api/admin/reservations")).andExpect(status().isOk());
    }

    @Test
    void connexionReussie() throws Exception {
        mvc.perform(post("/api/auth/login").param("email", "prof@ensa.ma").param("password", "prof123"))
           .andExpect(status().isOk());
    }

    @Test
    void mauvaisMotDePasseEstRefuse() throws Exception {
        mvc.perform(post("/api/auth/login").param("email", "prof@ensa.ma").param("password", "faux"))
           .andExpect(status().isUnauthorized());
    }
}
