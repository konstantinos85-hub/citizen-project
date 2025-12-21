package citizen.client;

import citizen.model.Citizen;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Profile;
import java.util.Scanner;

@Component
@Profile("!test") // Ασφάλεια 1: Spring Profile (Απενεργοποίηση στα tests)
public class CitizenClient implements CommandLineRunner {

    private final String BASE_URL = "http://localhost:8089/api/citizens";
    private final RestTemplate restTemplate;
    private final Scanner scanner = new Scanner(System.in);

    public CitizenClient() {
        // Υποστήριξη PATCH requests μέσω Apache HttpClient 5
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Override
    public void run(String... args) {
        // Ασφάλεια 2: Έλεγχος αν τρέχει στο περιβάλλον του GitHub Actions
        if (System.getenv("GITHUB_ACTIONS") != null) {
            return;
        }

        // Ασφάλεια 3: Έλεγχος Runtime αν η εντολή εκτέλεσης περιέχει JUnit ή το Integration Test
        String isTest = System.getProperty("sun.java.command", "");
        if (isTest.contains("junit") || isTest.contains("CitizenIT")) {
            return; 
        }

        boolean running = true;
        System.out.println("Ο Client ξεκίνησε επιτυχώς.");
        
        while (running) {
            try {
                System.out.println("\n--- ΜΕΝΟΥ ΔΙΑΧΕΙΡΙΣΗΣ ΠΟΛΙΤΩΝ (2025) ---");
                System.out.println("1. Λίστα | 2. Εύρεση | 3. Εισαγωγή | 4. Ενημέρωση | 5. Διαγραφή");
                System.out.println("Οποιαδήποτε άλλη τιμή για ΕΞΟΔΟΣ");
                System.out.print("Επιλογή: ");

                if (!scanner.hasNextLine()) {
                    break; 
                }
                
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
                System.out.println("Επιστροφή στο μενού...");
            }
        }
        System.out.println("Αντίο!");
    }

    private void listAll() {
        Citizen[] citizens = restTemplate.getForObject(BASE_URL + "/search", Citizen[].class);
        if (citizens != null) {
            for (Citizen c : citizens) {
                System.out.println(c.getAt() + ": " + c.getFirstName() + " " + c.getLastName());
            }
        }
    }

    private void getByAt() {
        System.out.print("Δώστε ΑΤ: ");
        String at = scanner.nextLine();
        try {
            Citizen c = restTemplate.getForObject(BASE_URL + "/" + at, Citizen.class);
            System.out.println("Στοιχεία: " + c.getFirstName() + " " + c.getLastName() + ", ΑΦΜ: " + c.getAfm());
        } catch (Exception e) {
            System.out.println("Ο πολίτης δεν βρέθηκε.");
        }
    }

    private void create() {
        Citizen c = new Citizen();
        System.out.print("ΑΤ: "); c.setAt(scanner.nextLine());
        System.out.print("Όνομα: "); c.setFirstName(scanner.nextLine());
        System.out.print("Επίθετο: "); c.setLastName(scanner.nextLine());
        System.out.print("Φύλο: "); c.setGender(scanner.nextLine());
        System.out.print("Ημ/νία Γέννησης (DD-MM-YYYY): "); c.setBirthDate(scanner.nextLine());
        System.out.print("ΑΦΜ: "); c.setAfm(scanner.nextLine());
        System.out.print("Διεύθυνση: "); c.setAddress(scanner.nextLine());

        restTemplate.postForObject(BASE_URL, c, Citizen.class);
        System.out.println("Επιτυχής εισαγωγή.");
    }

    private void update() {
        System.out.print("Δώστε ΑΤ για ενημέρωση: ");
        String at = scanner.nextLine();
        Citizen update = new Citizen();
        System.out.print("Νέο ΑΦΜ: "); update.setAfm(scanner.nextLine());
        System.out.print("Νέα Διεύθυνση: "); update.setAddress(scanner.nextLine());

        restTemplate.patchForObject(BASE_URL + "/" + at, update, String.class);
        System.out.println("Η ενημέρωση ολοκληρώθηκε.");
    }

    private void delete() {
        System.out.print("Δώστε ΑΤ για διαγραφή: ");
        String at = scanner.nextLine();
        restTemplate.delete(BASE_URL + "/" + at);
        System.out.println("Ο πολίτης διαγράφηκε.");
    }
}
