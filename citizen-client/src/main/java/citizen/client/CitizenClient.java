package citizen.client;

import citizen.model.Citizen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Profile;
import java.util.Arrays;
import java.util.Scanner;

@Component
@Profile("!test") 
public class CitizenClient implements CommandLineRunner {

    private final String BASE_URL = "http://localhost:8089/api/citizens";
    private final RestTemplate restTemplate;
    private Scanner scanner;

    @Autowired
    private Environment env;

    public CitizenClient() {
        // Υποστήριξη PATCH requests μέσω Apache HttpClient 5
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Override
    public void run(String... args) {
        // 1. ΡΙΖΙΚΗ ΛΥΣΗ: Έλεγχος αν η εκτέλεση ξεκίνησε από το CitizenIT ή JUnit
        // Αυτό σταματάει το μενού ακόμα και αν το Spring Profile αποτύχει
        boolean isTest = Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(element -> element.getClassName().contains("CitizenIT") || 
                                     element.getClassName().contains("junit") ||
                                     element.getClassName().contains("Surefire"));

        if (isTest) {
            return; 
        }

        // 2. ΑΣΦΑΛΕΙΑ: Έλεγχος για GitHub Actions ή περιβάλλον χωρίς τερματικό
        if (System.getenv("GITHUB_ACTIONS") != null || System.console() == null) {
            return; 
        }

        // 3. ΕΛΕΓΧΟΣ PROFILE: Αν το profile είναι "test", σταμάτα
        if (env != null && Arrays.asList(env.getActiveProfiles()).contains("test")) {
            return;
        }

        // Αν περάσει όλους τους ελέγχους, τότε μόνο αρχικοποιείται ο Scanner
        this.scanner = new Scanner(System.in);
        startApp();
    }

    private void startApp() {
        boolean running = true;
        System.out.println("Ο Client ξεκίνησε επιτυχώς (2025).");
        
        while (running) {
            try {
                System.out.println("\n--- ΜΕΝΟΥ ΔΙΑΧΕΙΡΙΣΗΣ ΠΟΛΙΤΩΝ ---");
                System.out.println("1. Λίστα | 2. Εύρεση | 3. Εισαγωγή | 4. Ενημέρωση | 5. Διαγραφή");
                System.out.println("Οποιαδήποτε άλλη τιμή για ΕΞΟΔΟΣ");
                System.out.print("Επιλογή: ");

                if (!scanner.hasNextLine()) break;
                
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> listAll();
                    case "2" -> getByAt();
                    case "3" -> create();
                    case "4" -> update();
                    case "5" -> delete();
                    default -> {
                        System.out.println("Τερματισμός εφαρμογής...");
                        running = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Σφάλμα: " + e.getMessage());
            }
        }
        System.out.println("Αντίο!");
    }

    private void listAll() {
        try {
            Citizen[] citizens = restTemplate.getForObject(BASE_URL + "/search", Citizen[].class);
            if (citizens != null) {
                for (Citizen c : citizens) {
                    System.out.println(c.getAt() + ": " + c.getFirstName() + " " + c.getLastName());
                }
            }
        } catch (Exception e) {
            System.out.println("Σφάλμα κατά την ανάκτηση.");
        }
    }

    private void getByAt() {
        System.out.print("Δώστε ΑΤ: ");
        String at = scanner.nextLine();
        try {
            Citizen c = restTemplate.getForObject(BASE_URL + "/" + at, Citizen.class);
            if (c != null) {
                System.out.println("Στοιχεία: " + c.getFirstName() + " " + c.getLastName() + ", ΑΦΜ: " + c.getAfm());
            }
        } catch (Exception e) {
            System.out.println("Ο πολίτης δεν βρέθηκε.");
        }
    }

    private void create() {
        try {
            Citizen c = new Citizen();
            System.out.print("ΑΤ: "); c.setAt(scanner.nextLine());
            System.out.print("Όνομα: "); c.setFirstName(scanner.nextLine());
            System.out.print("Επίθετο: "); c.setLastName(scanner.nextLine());
            System.out.print("Φύλο: "); c.setGender(scanner.nextLine());
            System.out.print("Ημ/νία Γέννησης: "); c.setBirthDate(scanner.nextLine());
            System.out.print("ΑΦΜ: "); c.setAfm(scanner.nextLine());
            System.out.print("Διεύθυνση: "); c.setAddress(scanner.nextLine());

            restTemplate.postForObject(BASE_URL, c, Citizen.class);
            System.out.println("Επιτυχής εισαγωγή.");
        } catch (Exception e) {
            System.out.println("Σφάλμα εισαγωγής.");
        }
    }

    private void update() {
        System.out.print("Δώστε ΑΤ για ενημέρωση: ");
        String at = scanner.nextLine();
        try {
            Citizen update = new Citizen();
            System.out.print("Νέο ΑΦΜ: "); update.setAfm(scanner.nextLine());
            System.out.print("Νέα Διεύθυνση: "); update.setAddress(scanner.nextLine());

            restTemplate.patchForObject(BASE_URL + "/" + at, update, String.class);
            System.out.println("Η ενημέρωση ολοκληρώθηκε.");
        } catch (Exception e) {
            System.out.println("Σφάλμα ενημέρωσης.");
        }
    }

    private void delete() {
        System.out.print("Δώστε ΑΤ για διαγραφή: ");
        String at = scanner.nextLine();
        try {
            restTemplate.delete(BASE_URL + "/" + at);
            System.out.println("Ο πολίτης διαγράφηκε.");
        } catch (Exception e) {
            System.out.println("Σφάλμα διαγραφής.");
        }
    }
}
