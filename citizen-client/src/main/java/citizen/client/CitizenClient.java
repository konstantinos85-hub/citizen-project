package citizen.client; 

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import citizen.model.Citizen;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Profile;
import java.util.Scanner;

@Component
@Profile("!test") // Ασφάλεια 1: Δεν φορτώνεται ποτέ όταν τρέχουν τα tests
@ConditionalOnProperty(name = "citizen.client.enabled", havingValue = "true", matchIfMissing = true)
public class CitizenClient implements CommandLineRunner {

    private final String BASE_URL = "http://localhost:8089/api/citizens";
    private final RestTemplate restTemplate;

    public CitizenClient() {
        // Υποστήριξη PATCH requests μέσω Apache HttpClient 5 (απαραίτητο για το 2025)
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Override
    public void run(String... args) {
        // Ασφάλεια 2: Έλεγχος αν υπάρχει πραγματικό τερματικό (πληκτρολόγιο)
        // Στο GitHub Actions και στο Terraform η System.console() επιστρέφει null
        if (System.console() == null || System.getenv("GITHUB_ACTIONS") != null) {
            return; 
        }

        // Ασφάλεια 3: Έλεγχος αν η εντολή εκτέλεσης περιέχει λέξεις-κλειδιά δοκιμών
        String command = System.getProperty("sun.java.command", "").toLowerCase();
        if (command.contains("junit") || command.contains("citizenit") || command.contains("surefire")) {
            return; 
        }

        // Αν περάσει όλους τους ελέγχους, ξεκινάει το μενού
        startInteractiveMenu();
    }

    private void startInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        System.out.println("=== Ο Client ξεκίνησε επιτυχώς (2025) ===");
        
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
                    case "2" -> getByAt(scanner);
                    case "3" -> create(scanner);
                    case "4" -> update(scanner);
                    case "5" -> delete(scanner);
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
        // Κλείνουμε τον scanner μόνο εδώ στο τέλος
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
            System.out.println("Σφάλμα σύνδεσης. Βεβαιωθείτε ότι ο Server τρέχει.");
        }
    }

    private void getByAt(Scanner scanner) {
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

    private void create(Scanner scanner) {
        try {
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
        } catch (Exception e) {
            System.out.println("Σφάλμα κατά την εισαγωγή.");
        }
    }

    private void update(Scanner scanner) {
        System.out.print("Δώστε ΑΤ για ενημέρωση: ");
        String at = scanner.nextLine();
        try {
            Citizen update = new Citizen();
            System.out.print("Νέο ΑΦΜ: "); update.setAfm(scanner.nextLine());
            System.out.print("Νέα Διεύθυνση: "); update.setAddress(scanner.nextLine());

            restTemplate.patchForObject(BASE_URL + "/" + at, update, String.class);
            System.out.println("Η ενημέρωση ολοκληρώθηκε.");
        } catch (Exception e) {
            System.out.println("Σφάλμα κατά την ενημέρωση.");
        }
    }

    private void delete(Scanner scanner) {
        System.out.print("Δώστε ΑΤ για διαγραφή: ");
        String at = scanner.nextLine();
        try {
            restTemplate.delete(BASE_URL + "/" + at);
            System.out.println("Ο πολίτης διαγράφηκε.");
        } catch (Exception e) {
            System.out.println("Σφάλμα κατά τη διαγραφή.");
        }
    }
}
