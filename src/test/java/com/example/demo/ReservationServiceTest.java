package com.example.demo;

import com.example.demo.dto.Dtos.AnnonceRequest;
import com.example.demo.dto.Dtos.ReservationRequest;
import com.example.demo.dto.Dtos.ReservationView;
import com.example.demo.entity.Reservation;
import com.example.demo.repository.SalleRepository;
import com.example.demo.service.AnnonceService;
import com.example.demo.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:servicedb")
@Transactional
class ReservationServiceTest {

    private static final String PROF = "prof@ensa.ma";
    private static final String JUSTIF = "Séance non assurée : rattrapage nécessaire pour respecter le programme.";
    /** Salle libre de tout cours et de toute réservation dans les données de démonstration. */
    private static final String SALLE_TEST = "F-14";

    @Autowired ReservationService service;
    @Autowired AnnonceService annonces;
    @Autowired SalleRepository salles;

    private static LocalDate mercredi() { return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)); }
    private static LocalDate lundi() { return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)); }

    private ReservationRequest req(String salle, LocalDate jour, int h1, int h2, int nb, Reservation.Type type,
                                   String niveau, boolean urgent, boolean notifier) {
        Long id = salles.findByNom(salle).orElseThrow().getId();
        return new ReservationRequest(id, jour.atTime(h1, 0), jour.atTime(h2, 0), "DevOps", niveau, nb,
                type, JUSTIF, urgent, notifier);
    }

    private ReservationRequest simple(String salle, LocalDate jour, int h1, int h2, int nb) {
        return req(salle, jour, h1, h2, nb, Reservation.Type.COURS_RATTRAPAGE, "MGSI3", false, false);
    }

    @Test
    void creationEnAttenteAvecObjetEtPriorite() {
        ReservationView v = service.creer(PROF, simple(SALLE_TEST, mercredi(), 15, 17, 30));
        assertEquals("EN_ATTENTE", v.statut());
        assertEquals(3, v.priorite());
        assertEquals("Rattrapage — DevOps (MGSI3)", v.objet());
        assertEquals("MGSI3", v.niveau());
    }

    @Test
    void urgenceAugmenteLaPriorite() {
        ReservationView v = service.creer(PROF, req(SALLE_TEST, mercredi(), 15, 17, 30,
                Reservation.Type.COURS_RATTRAPAGE, "MGSI3", true, false));
        assertEquals(4, v.priorite());
        assertTrue(v.urgent());
    }

    @Test
    void soutenanceEstPrioritaire() {
        ReservationView v = service.creer(PROF, req("C-16", mercredi(), 9, 11, 10,
                Reservation.Type.SOUTENANCE_THESE, "DOCT", false, false));
        assertEquals(4, v.priorite());
    }

    @Test
    void creneauConfirmeNePeutPasEtreReserveDeuxFois() {
        ReservationRequest r = simple(SALLE_TEST, mercredi(), 15, 17, 30);
        service.confirmer(service.creer(PROF, r).id());
        assertThrows(IllegalStateException.class, () -> service.creer(PROF, r));
    }

    @Test
    void salleOccupeeParUnCoursEstRefusee() {
        // A-01 : cours de Génie logiciel et qualité le lundi 8h30-10h30
        assertThrows(IllegalStateException.class,
                () -> service.creer(PROF, req("A-01", lundi(), 9, 10, 20, Reservation.Type.REUNION, "ENS", false, false)));
    }

    @Test
    void capaciteInsuffisanteEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> service.creer(PROF, simple("A-01", mercredi(), 15, 16, 500)));
    }

    @Test
    void horairesHorsPlageEtDimancheSontRefuses() {
        assertThrows(IllegalArgumentException.class, () -> service.creer(PROF, simple(SALLE_TEST, mercredi(), 6, 7, 10)));
        assertThrows(IllegalArgumentException.class, () -> service.creer(PROF, simple(SALLE_TEST, mercredi(), 19, 21, 10)));
        LocalDate dimanche = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        assertThrows(IllegalArgumentException.class, () -> service.creer(PROF, simple(SALLE_TEST, dimanche, 10, 11, 10)));
    }

    @Test
    void niveauInconnuEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> service.creer(PROF,
                req(SALLE_TEST, mercredi(), 15, 17, 30, Reservation.Type.REUNION, "GI3", false, false)));
    }

    @Test
    void unAutreUtilisateurNePeutPasAnnuler() {
        ReservationView v = service.creer(PROF, simple(SALLE_TEST, mercredi(), 15, 17, 30));
        assertThrows(SecurityException.class, () -> service.annuler("admin@ensa.ma", v.id()));
        service.annuler(PROF, v.id());
        assertEquals("ANNULEE", service.mesReservations(PROF).stream()
                .filter(x -> x.id().equals(v.id())).findFirst().orElseThrow().statut());
    }

    @Test
    void demandeEnvoyeeNotifieLAdministrationEtLEnseignante() {
        int avantAdmin = service.notifications("admin@ensa.ma").size();
        service.creer(PROF, req(SALLE_TEST, mercredi(), 15, 17, 30, Reservation.Type.COURS_RATTRAPAGE, "MGSI3", true, false));
        var notifsAdmin = service.notifications("admin@ensa.ma");
        assertEquals(avantAdmin + 1, notifsAdmin.size());
        assertTrue(notifsAdmin.get(0).message().startsWith("URGENT"));
        assertTrue(service.notifications(PROF).get(0).message().contains("transmise à l'administration"));
    }

    @Test
    void refusEnregistreLeMotifEtNotifieLEnseignante() {
        ReservationView v = service.creer(PROF, simple(SALLE_TEST, mercredi(), 15, 17, 30));
        service.refuser(v.id(), "Salle indisponible");
        assertTrue(service.notifications(PROF).stream().anyMatch(n -> n.message().contains("Salle indisponible")));
    }

    @Test
    void confirmationInformeLesEtudiantsDuNiveauSiDemande() {
        String etudiantMgsi3 = "hajar.ziani@etu.ensa.ma";
        String etudiantMgsi2 = "amine.elidrissi@etu.ensa.ma";
        int avant3 = service.notifications(etudiantMgsi3).size();
        int avant2 = service.notifications(etudiantMgsi2).size();

        ReservationView v = service.creer(PROF, req(SALLE_TEST, mercredi(), 15, 17, 30,
                Reservation.Type.COURS_RATTRAPAGE, "MGSI3", false, true));
        service.confirmer(v.id());

        assertEquals(avant3 + 1, service.notifications(etudiantMgsi3).size());
        assertTrue(service.notifications(etudiantMgsi3).get(0).message().contains("rattrapage"));
        assertEquals(avant2, service.notifications(etudiantMgsi2).size());
    }

    @Test
    void sansOptionLesEtudiantsNeSontPasInformes() {
        String etudiant = "hajar.ziani@etu.ensa.ma";
        int avant = service.notifications(etudiant).size();
        service.confirmer(service.creer(PROF, simple(SALLE_TEST, mercredi(), 15, 17, 30)).id());
        assertEquals(avant, service.notifications(etudiant).size());
    }

    @Test
    void annonceAuGroupeNotifieSesEtudiants() {
        var vue = annonces.envoyer(PROF, new AnnonceRequest("MGSI3", "Le TP de vendredi est reporté à lundi."));
        assertEquals(5, vue.nbDestinataires());
        assertTrue(service.notifications("sara.moussaoui@etu.ensa.ma").get(0).message().contains("reporté"));
        assertThrows(IllegalArgumentException.class, () -> annonces.envoyer(PROF, new AnnonceRequest("XX9", "Message de test")));
    }
}
