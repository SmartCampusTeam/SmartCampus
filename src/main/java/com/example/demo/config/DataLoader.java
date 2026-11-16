package com.example.demo.config;

import com.example.demo.entity.*;
import com.example.demo.entity.Batiment.Forme;
import com.example.demo.entity.Batiment.TypeBatiment;
import com.example.demo.entity.Reservation.Statut;
import com.example.demo.entity.Salle.TypeSalle;
import com.example.demo.entity.Utilisateur.Role;
import com.example.demo.repository.*;
import com.example.demo.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.demo.entity.Salle.TypeSalle.*;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private static final String FILIERE = "Management et Gouvernance des SI";

    // Emprises (haut-gauche ; haut-droit ; bas-droit ; bas-gauche) relevées sur l'imagerie satellite.
    private static final String POLY_A = "32.8968839,-6.9137913;32.8968448,-6.9135273;32.8966447,-6.9135667;32.8966839,-6.9138307";
    private static final String POLY_B = "32.896837,-6.9134754;32.8967985,-6.9132157;32.8966057,-6.9132537;32.8966442,-6.9135134";
    private static final String POLY_C = "32.8970662,-6.9134303;32.8970277,-6.9131706;32.8968349,-6.9132086;32.8968734,-6.9134683";
    private static final String POLY_D = "32.8973581,-6.9137069;32.8973164,-6.9134256;32.8970763,-6.9134729;32.897118,-6.9137542";
    private static final String POLY_F = "32.8974514,-6.9129982;32.8974128,-6.9127385;32.8971655,-6.9127872;32.897204,-6.9130469";
    private static final String POLY_AMPHI1 = "32.8976269,-6.9136763;32.8975915,-6.9134383;32.897446,-6.9134669;32.8974814,-6.913705";
    private static final String POLY_AMPHI2 = "32.8976251,-6.9133114;32.8975879,-6.9130604;32.8973987,-6.9130976;32.897436,-6.9133486";
    private static final String POLY_AT = "32.8969407,-6.9145775;32.896897,-6.9142832;32.8966497,-6.9143319;32.8966933,-6.9146262";

    // Cellules de la grille 4 x 2 de chaque département : {colonne, ligne, largeur, hauteur}
    private static final int[][] CELLS_RDC = {{0, 0, 1, 1}, {1, 0, 1, 1}, {2, 0, 2, 1}, {0, 1, 1, 1}, {1, 1, 1, 1}, {2, 1, 2, 1}};
    private static final int[][] CELLS_ETAGE = {{0, 0, 2, 1}, {2, 0, 1, 1}, {3, 0, 1, 1}, {0, 1, 1, 1}, {1, 1, 1, 1}, {2, 1, 2, 1}};

    // Créneaux types : {heure début, min début, heure fin, min fin}
    private static final int[][] CRENEAUX = {{8, 30, 10, 30}, {10, 30, 12, 30}, {14, 0, 16, 0}, {16, 0, 18, 0}};

    // Salles réservées pour des événements précis (pas de cours récurrents générés dedans)
    private static final Set<String> EVENEMENTIELLES = Set.of("C-06", "D-02", "A-11", "A-12", "A-14", "A-15", "B-06",
            "C-03", "C-16", "D-05", "F-11", "F-12", "F-14", "F-15", "Amphi Théâtre");

    private static final Reservation.Type T_RATTRAPAGE = Reservation.Type.COURS_RATTRAPAGE;
    private static final Reservation.Type T_REUNION = Reservation.Type.REUNION;
    private static final Reservation.Type T_FORMATION = Reservation.Type.FORMATION;
    private static final Reservation.Type T_SOUTENANCE = Reservation.Type.SOUTENANCE_THESE;

    private final UtilisateurRepository users;
    private final GroupeRepository groupes;
    private final BatimentRepository batiments;
    private final SalleRepository salles;
    private final SeanceRepository seances;
    private final ReservationRepository reservations;
    private final NotificationRepository notifications;
    private final AnnonceRepository annonces;
    private final PasswordEncoder encoder;

    private final Set<String> occupe = new HashSet<>();

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        String mdpProf = encoder.encode("prof123");
        String mdpEtudiant = encoder.encode("etu123");

        // ---------- Groupes de la filière MGSI ----------
        groupe("MGSI1", "MGSI — 1re année", 42);
        groupe("MGSI2", "MGSI — 2e année", 38);
        groupe("MGSI3", "MGSI — 3e année (Management et Gouvernance des Systèmes d'Information)", 35);
        groupe("MASTER", "Master Systèmes d'Information", 22);
        groupe("DOCT", "Doctorants", 15);
        groupe("ENS", "Équipe pédagogique", 12);

        // ---------- Utilisateurs ----------
        Utilisateur karoum = utilisateur("KAROUM", "Bouchra", "prof@ensa.ma", mdpProf, Role.ENSEIGNANT,
                FILIERE, "Professeure", "06 61 23 45 67", "Bureau 12 — Département A",
                "Professeure de DevOps et de gouvernance des systèmes d'information. Centres d'intérêt : CI/CD, conteneurisation, DevSecOps.", null);
        Utilisateur elAmrani = utilisateur("EL AMRANI", "Youssef", "youssef.elamrani@ensa.ma", mdpProf, Role.ENSEIGNANT,
                FILIERE, "Professeur", "06 62 11 22 33", "Bureau 8 — Département D", null, null);
        Utilisateur benchekroun = utilisateur("BENCHEKROUN", "Nadia", "nadia.benchekroun@ensa.ma", mdpProf, Role.ENSEIGNANT,
                "Sciences de gestion", "Professeure", "06 63 44 55 66", "Bureau 3 — Département C", null, null);
        Utilisateur tazi = utilisateur("TAZI", "Hicham", "hicham.tazi@ensa.ma", mdpProf, Role.ENSEIGNANT,
                "Mathématiques appliquées", "Professeur", "06 64 77 88 99", "Bureau 15 — Département F", null, null);
        utilisateur("BENNANI", "Salma", "admin@ensa.ma", encoder.encode("admin123"), Role.ADMIN,
                "Administration", "Directrice adjointe", null, null, null, null);

        etudiants("MGSI2", mdpEtudiant, new String[][]{{"Amine", "EL IDRISSI"}, {"Salma", "BENANI"}, {"Yassine", "OUALI"},
                {"Imane", "CHRAIBI"}, {"Omar", "LAHLOU"}});
        etudiants("MGSI3", mdpEtudiant, new String[][]{{"Hajar", "ZIANI"}, {"Mehdi", "BERRADA"}, {"Khadija", "ALAMI"},
                {"Anas", "FASSI"}, {"Sara", "MOUSSAOUI"}});
        etudiants("MASTER", mdpEtudiant, new String[][]{{"Reda", "KETTANI"}, {"Meryem", "SQALLI"}, {"Ayoub", "TAHIRI"},
                {"Lamia", "BOUZIDI"}});

        // ---------- Campus : positions des repères = coordonnées du fichier KML ----------
        departement("A", 32.8967576, -6.9136855, "#f57c00", POLY_A,
                new TypeSalle[]{SALLE_COURS, SALLE_COURS, LABO, SALLE_COURS, SALLE_COURS, REUNION},
                new TypeSalle[]{LABO, SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, REUNION});
        departement("B", 32.8967025, -6.9133702, "#ab47bc", POLY_B,
                new TypeSalle[]{SALLE_COURS, SALLE_COURS, LABO, SALLE_COURS, SALLE_COURS, SOUTENANCE},
                new TypeSalle[]{SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, REUNION});
        departement("C", 32.8970015, -6.9133330, "#42a5f5", POLY_C,
                new TypeSalle[]{SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, REUNION},
                new TypeSalle[]{LABO, SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, SOUTENANCE});
        departement("D", 32.8972674, -6.9135893, "#388e3c", POLY_D,
                new TypeSalle[]{SALLE_COURS, SALLE_COURS, LABO, SALLE_COURS, SALLE_COURS, REUNION},
                new TypeSalle[]{LABO, SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, SOUTENANCE});
        departement("F", 32.8973209, -6.9128834, "#7b1fa2", POLY_F,
                new TypeSalle[]{SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, REUNION},
                new TypeSalle[]{LABO, SALLE_COURS, SALLE_COURS, SALLE_COURS, SALLE_COURS, REUNION});

        amphi("AMPHI1", "Amphi 1", 32.8975677, -6.9136169, POLY_AMPHI1, Forme.RECT, "#1976d2", 200,
                "Vidéoprojecteur, sono, climatisation");
        amphi("AMPHI2", "Amphi 2", 32.8975306, -6.9132064, POLY_AMPHI2, Forme.RECT, "#1976d2", 150,
                "Vidéoprojecteur, sono");
        amphi("AT", "Amphi Théâtre", 32.8968100, -6.9144504, POLY_AT, Forme.CERCLE, "#303f9f", 300,
                "Sono, scène, gradins");

        // ---------- Emploi du temps de Pr. Karoum et de quelques collègues ----------
        seance(karoum, "Génie logiciel et qualité", "MGSI2", DayOfWeek.MONDAY, 8, 30, 10, 30, "A-01");
        seance(karoum, "DevOps", "MGSI3", DayOfWeek.TUESDAY, 10, 30, 12, 30, "A-03");
        seance(karoum, "Conteneurisation et Kubernetes", "MASTER", DayOfWeek.WEDNESDAY, 14, 0, 16, 0, "B-02");
        seance(karoum, "Architecture des SI", "MGSI3", DayOfWeek.THURSDAY, 8, 30, 10, 30, "Amphi 2");
        seance(karoum, "Gouvernance des SI", "MGSI3", DayOfWeek.FRIDAY, 10, 30, 12, 30, "C-12");

        seance(elAmrani, "Programmation Java", "MGSI2", DayOfWeek.MONDAY, 10, 30, 12, 30, "D-01");
        seance(elAmrani, "Réseaux et sécurité", "MGSI3", DayOfWeek.TUESDAY, 8, 30, 10, 30, "D-11");
        seance(benchekroun, "Recherche opérationnelle", "MGSI2", DayOfWeek.MONDAY, 14, 0, 16, 0, "C-02");
        seance(benchekroun, "Gestion de projet", "MGSI3", DayOfWeek.WEDNESDAY, 8, 30, 10, 30, "C-12");
        seance(tazi, "Probabilités et statistiques", "MGSI2", DayOfWeek.THURSDAY, 10, 30, 12, 30, "Amphi 1");
        seance(tazi, "Algèbre linéaire", "MGSI1", DayOfWeek.TUESDAY, 14, 0, 16, 0, "F-02");

        // Cours récurrents des autres enseignants : une partie des salles est occupée à chaque créneau
        genererEmploiDuTemps(List.of(elAmrani, benchekroun, tazi));

        // ---------- Réservations : un déroulement réaliste ----------
        LocalDate j1 = ouvre(LocalDate.now().plusDays(1));   // = date proposée par défaut sur la carte
        LocalDate j2 = ouvre(LocalDate.now().plusDays(2));
        LocalDate j3 = ouvre(LocalDate.now().plusDays(3));
        LocalDate j4 = ouvre(LocalDate.now().plusDays(4));
        LocalDate j5 = ouvre(LocalDate.now().plusDays(5));

        // Demain matin : des salles confirmées (rouge) et des demandes en attente (orange)
        resa(benchekroun, "Amphi Théâtre", j1.atTime(8, 30), j1.atTime(12, 30), T_FORMATION, Statut.CONFIRMEE,
                "Journée d'accueil et d'intégration", "MGSI1", 120,
                "Présentation de la filière, des outils numériques et des règles de vie du campus pour les nouveaux étudiants.",
                false, false, null, 120);
        resa(tazi, "B-06", j1.atTime(8, 30), j1.atTime(10, 30), T_SOUTENANCE, Statut.CONFIRMEE,
                "Soutenance de thèse de Mme Alaoui", "DOCT", 20,
                "Soutenance publique, salle équipée de la visioconférence pour un rapporteur externe.",
                false, false, null, 150);
        resa(elAmrani, "F-11", j1.atTime(8, 30), j1.atTime(10, 30), T_FORMATION, Statut.CONFIRMEE,
                "Atelier Python pour la data", "MASTER", 22,
                "Atelier pratique sur postes équipés, nécessaire avant le mini-projet de fin de semestre.",
                false, true, null, 100);
        resa(elAmrani, "C-03", j1.atTime(9, 0), j1.atTime(11, 0), T_RATTRAPAGE, Statut.CONFIRMEE,
                "Réseaux et sécurité", "MGSI3", 30,
                "Rattrapage de la séance du mardi dernier annulée pour cause de maintenance du réseau.",
                false, true, null, 80);
        resa(tazi, "D-05", j1.atTime(8, 30), j1.atTime(10, 30), T_REUNION, Statut.EN_ATTENTE,
                "Commission pédagogique", "ENS", 8,
                "Préparation du conseil de filière et répartition des charges d'enseignement.",
                false, false, null, 6);
        resa(elAmrani, "D-05", j1.atTime(9, 0), j1.atTime(11, 0), T_REUNION, Statut.EN_ATTENTE,
                "Réunion de projet PFE", "ENS", 6,
                "Point d'étape avec les encadrants avant les pré-soutenances.",
                false, false, null, 4);
        resa(benchekroun, "A-14", j1.atTime(9, 0), j1.atTime(11, 0), T_RATTRAPAGE, Statut.EN_ATTENTE,
                "Gestion de projet", "MGSI3", 30,
                "Séance manquée pour cause de déplacement professionnel ; le rattrapage doit avoir lieu avant la remise du projet.",
                true, true, null, 3);

        // Les demandes de Pr. Karoum, à tous les stades
        resa(karoum, "C-06", j1.atTime(14, 0), j1.atTime(16, 0), T_REUNION, Statut.CONFIRMEE,
                "Réunion pédagogique du module DevOps", "ENS", 8,
                "Coordination des TP de DevOps avec les collègues intervenant dans la filière MGSI.",
                false, false, null, 52);
        resa(karoum, "D-02", j3.atTime(10, 30), j3.atTime(12, 30), T_RATTRAPAGE, Statut.EN_ATTENTE,
                "DevOps — Intégration continue", "MGSI3", 35,
                "Séance du mardi non assurée (participation à un jury de thèse à Fès). Le rattrapage est nécessaire pour respecter l'avancement du TP Jenkins avant l'examen.",
                false, true, null, 5);
        resa(elAmrani, "D-02", j3.atTime(10, 0), j3.atTime(12, 0), T_REUNION, Statut.EN_ATTENTE,
                "Réunion de suivi des PFE", "ENS", 8,
                "Point d'avancement mensuel avec les encadrants des projets de fin d'études.",
                false, false, null, 9);
        resa(karoum, "A-11", j2.atTime(14, 0), j2.atTime(16, 0), T_FORMATION, Statut.EN_ATTENTE,
                "Atelier Jenkins et Docker", "MASTER", 22,
                "Atelier pratique indispensable avant l'évaluation du projet DevOps de vendredi : le laboratoire dispose des postes et du réseau nécessaires.",
                true, true, null, 2);
        resa(karoum, "C-16", j5.atTime(14, 0), j5.atTime(17, 0), T_FORMATION, Statut.REFUSEE,
                "Formation GitHub Actions", "MASTER", 20,
                "Session de formation complémentaire sur l'automatisation des workflows.", false, false,
                "Soutenance de thèse programmée sur ce créneau dans cette salle.", 30);
        resa(karoum, "A-15", j4.atTime(10, 0), j4.atTime(12, 0), T_FORMATION, Statut.ANNULEE,
                "Atelier Terraform", "MASTER", 18,
                "Initiation à l'infrastructure as code, annulée car reportée à la semaine suivante.", false, false, null, 60);
        resa(tazi, "C-16", j5.atTime(14, 0), j5.atTime(17, 0), T_SOUTENANCE, Statut.CONFIRMEE,
                "Soutenance de thèse de M. Idrissi", "DOCT", 18,
                "Soutenance publique devant jury, salle équipée en visioconférence pour un membre du jury à distance.",
                false, false, null, 72);
        resa(benchekroun, "Amphi Théâtre", j2.atTime(15, 0), j2.atTime(18, 0), T_FORMATION, Statut.CONFIRMEE,
                "Conférence Industrie 4.0", "MGSI3", 120,
                "Conférence invitée ouverte aux étudiants de dernière année, forte affluence attendue.",
                false, false, null, 96);
        resa(elAmrani, "F-12", j4.atTime(8, 30), j4.atTime(10, 30), T_RATTRAPAGE, Statut.CONFIRMEE,
                "Réseaux et sécurité", "MGSI3", 30,
                "Rattrapage de la séance du mardi annulée pour cause de maintenance du laboratoire réseau.",
                false, true, null, 40);
        resa(tazi, "A-12", j2.atTime(10, 0), j2.atTime(12, 0), T_REUNION, Statut.EN_ATTENTE,
                "Jury de module", "ENS", 6,
                "Délibération du jury de module de probabilités et statistiques.", false, false, null, 7);

        // ---------- Notifications et annonce de démonstration ----------
        notif(karoum, "Bienvenue sur SmartCampus, Pr. Karoum : réservez vos salles en quelques clics.");
        notif(karoum, "✔ Votre réservation de C-06 est confirmée.");
        notif(karoum, "✖ Votre réservation de C-16 est refusée. Motif : Soutenance de thèse programmée sur ce créneau dans cette salle.");
        annonce(karoum, "MGSI3", "Rappel : le TP Jenkins est à déposer sur GitHub avant vendredi 23h59. Pensez à joindre votre Jenkinsfile.");
    }

    // ---------- Emploi du temps généré ----------

    /** Occupe environ 40 % des salles à chaque créneau, du lundi au samedi, sans toucher aux salles événementielles. */
    private void genererEmploiDuTemps(List<Utilisateur> profs) {
        String[][] matieres = {
                {"Programmation Java", "Bases de données avancées", "Développement web", "Réseaux et sécurité"},
                {"Gestion de projet", "Management des organisations", "Recherche opérationnelle", "Audit des SI"},
                {"Probabilités et statistiques", "Analyse de données", "Algèbre linéaire", "Mathématiques financières"}};
        String[] niveaux = {"MGSI1", "MGSI2", "MGSI3", "MASTER"};
        DayOfWeek[] jours = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

        List<Salle> candidates = salles.findAll().stream()
                .filter(s -> !EVENEMENTIELLES.contains(s.getNom()))
                .sorted(Comparator.comparing(Salle::getNom))
                .toList();

        List<Seance> lot = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            for (int d = 0; d < jours.length; d++) {
                for (int c = 0; c < CRENEAUX.length; c++) {
                    if ((i * 7 + d * 5 + c * 3) % 10 >= 4) continue;
                    Salle salle = candidates.get(i);
                    if (occupe.contains(cle(salle.getNom(), jours[d], c))) continue;
                    int p = (i + d + c) % profs.size();
                    Seance s = new Seance();
                    s.setEnseignant(profs.get(p));
                    s.setMatiere(matieres[p][(i + d) % matieres[p].length]);
                    s.setGroupe(niveaux[(i + c) % niveaux.length]);
                    s.setJour(jours[d]);
                    s.setHeureDebut(LocalTime.of(CRENEAUX[c][0], CRENEAUX[c][1]));
                    s.setHeureFin(LocalTime.of(CRENEAUX[c][2], CRENEAUX[c][3]));
                    s.setSalle(salle);
                    lot.add(s);
                }
            }
        }
        seances.saveAll(lot);
    }

    private static String cle(String salle, DayOfWeek jour, int creneau) {
        return salle + "|" + jour + "|" + creneau;
    }

    // ---------- Fabriques ----------

    private void groupe(String code, String libelle, int effectif) {
        Groupe g = new Groupe();
        g.setCode(code); g.setLibelle(libelle); g.setEffectif(effectif);
        groupes.save(g);
    }

    private Utilisateur utilisateur(String nom, String prenom, String email, String hash, Role role, String dep,
                                    String grade, String tel, String bureau, String bio, String groupe) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom); u.setPrenom(prenom); u.setEmail(email); u.setMotDePasse(hash); u.setRole(role);
        u.setDepartement(dep); u.setGrade(grade); u.setTelephone(tel); u.setBureau(bureau); u.setBio(bio);
        u.setGroupe(groupe);
        return users.save(u);
    }

    private void etudiants(String groupe, String hash, String[][] noms) {
        for (String[] n : noms) {
            String email = (n[0] + "." + n[1]).toLowerCase().replace(" ", "") + "@etu.ensa.ma";
            utilisateur(n[1], n[0], email, hash, Role.ETUDIANT, FILIERE, "Étudiant(e) " + groupe,
                    null, null, null, groupe);
        }
    }

    /** Département : 2 étages, grille 4 colonnes x 2 lignes, 6 salles par étage. */
    private void departement(String code, double lat, double lng, String couleur, String poly,
                             TypeSalle[] rdc, TypeSalle[] etage) {
        Batiment b = batiment(code, "Département " + code, TypeBatiment.DEPARTEMENT, Forme.RECT,
                lat, lng, poly, 2, 4, 2, couleur);
        for (int i = 0; i < 6; i++) salle(b, code + "-0" + (i + 1), 0, CELLS_RDC[i], rdc[i], i + 1);
        for (int i = 0; i < 6; i++) salle(b, code + "-1" + (i + 1), 1, CELLS_ETAGE[i], etage[i], i + 1);
    }

    private void amphi(String code, String nom, double lat, double lng, String poly, Forme forme,
                       String couleur, int capacite, String equip) {
        Batiment b = batiment(code, nom, TypeBatiment.AMPHI, forme, lat, lng, poly, 1, 1, 1, couleur);
        creerSalle(b, nom, 0, new int[]{0, 0, 1, 1}, AMPHI, capacite, equip);
    }

    private Batiment batiment(String code, String nom, TypeBatiment type, Forme forme, double lat, double lng,
                              String poly, int etages, int cols, int rows, String couleur) {
        Batiment b = new Batiment();
        b.setCode(code); b.setNom(nom); b.setType(type); b.setForme(forme);
        b.setLatitude(lat); b.setLongitude(lng); b.setPolygone(poly);
        b.setNbEtages(etages); b.setGridCols(cols); b.setGridRows(rows); b.setCouleur(couleur);
        return batiments.save(b);
    }

    private void salle(Batiment b, String nom, int etage, int[] cell, TypeSalle type, int idx) {
        boolean large = cell[2] == 2;
        int cap = switch (type) {
            case SALLE_COURS -> large ? 60 : (idx % 2 == 0 ? 36 : 40);
            case LABO -> large ? 30 : 24;
            case REUNION -> 14;
            case SOUTENANCE -> 25;
            case AMPHI -> 150;
        };
        String equip = switch (type) {
            case SALLE_COURS -> "Vidéoprojecteur, tableau";
            case LABO -> cap + " PC, vidéoprojecteur, réseau";
            case REUNION -> "Écran, visioconférence";
            case SOUTENANCE -> "Vidéoprojecteur, visioconférence, micro";
            case AMPHI -> "Vidéoprojecteur, sono";
        };
        creerSalle(b, nom, etage, cell, type, cap, equip);
    }

    private void creerSalle(Batiment b, String nom, int etage, int[] cell, TypeSalle type, int cap, String equip) {
        Salle s = new Salle();
        s.setNom(nom); s.setBatiment(b); s.setEtage(etage); s.setType(type);
        s.setCapacite(cap); s.setEquipements(equip);
        s.setColonne(cell[0]); s.setLigne(cell[1]); s.setLargeur(cell[2]); s.setHauteur(cell[3]);
        salles.save(s);
    }

    private void seance(Utilisateur ens, String matiere, String groupe, DayOfWeek jour,
                        int h1, int m1, int h2, int m2, String nomSalle) {
        Seance s = new Seance();
        s.setEnseignant(ens); s.setMatiere(matiere); s.setGroupe(groupe); s.setJour(jour);
        s.setHeureDebut(LocalTime.of(h1, m1)); s.setHeureFin(LocalTime.of(h2, m2));
        s.setSalle(salles.findByNom(nomSalle).orElseThrow());
        seances.save(s);
        for (int c = 0; c < CRENEAUX.length; c++) {
            if (CRENEAUX[c][0] == h1 && CRENEAUX[c][1] == m1) occupe.add(cle(nomSalle, jour, c));
        }
    }

    private void resa(Utilisateur u, String salle, LocalDateTime d, LocalDateTime f, Reservation.Type type,
                      Statut statut, String matiere, String niveau, int nb, String justification,
                      boolean urgent, boolean notif, String motif, int ilYaHeures) {
        Reservation r = new Reservation();
        r.setDemandeur(u);
        r.setSalle(salles.findByNom(salle).orElseThrow());
        r.setDateDebut(d); r.setDateFin(f);
        r.setMatiere(matiere); r.setNiveau(niveau); r.setJustification(justification);
        r.setUrgent(urgent); r.setNotifierEtudiants(notif);
        r.setNbParticipants(nb); r.setType(type); r.setStatut(statut); r.setMotifRefus(motif);
        r.setObjet(ReservationService.libelle(type) + " — " + matiere + " (" + niveau + ")");
        r.setPriorite(ReservationService.priorite(type, urgent));
        r.setDateDemande(LocalDateTime.now().minusHours(ilYaHeures));
        reservations.save(r);
    }

    private void notif(Utilisateur u, String message) {
        Notification n = new Notification();
        n.setDestinataire(u);
        n.setMessage(message);
        notifications.save(n);
    }

    private void annonce(Utilisateur auteur, String groupe, String message) {
        List<Utilisateur> dest = users.findByRoleAndGroupe(Role.ETUDIANT, groupe);
        Annonce a = new Annonce();
        a.setAuteur(auteur); a.setGroupe(groupe); a.setMessage(message);
        a.setNbDestinataires(dest.size());
        a.setDateEnvoi(LocalDateTime.now().minusDays(1));
        annonces.save(a);
        dest.forEach(e -> notif(e, "Annonce de Pr. " + auteur.getNom() + " (" + groupe + ") : " + message));
    }

    private static LocalDate ouvre(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }
}