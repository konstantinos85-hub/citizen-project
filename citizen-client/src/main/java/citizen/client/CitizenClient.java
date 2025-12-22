package citizen.client; 

import citizen.model.Citizen;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Scanner;

@Profile("!test")
@Component
 // Ασφάλεια 1: Δεν φορτώνεται η κλάση αν το ενεργό profile είναι "test"
@ConditionalOnProperty(name = "citizen.client.enabled", havingValue = "true", matchIfMissing = true)
public class CitizenClient implements CommandLineRunner {

    private final String BASE_URL = "http://localhost:8089/api/citizens";
    private final RestTemplate restTemplate;

    public CitizenClient() {
        // Χρήση Apache HttpClient για υποστήριξη PATCH requests (απαραίτητο για το 2025)
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Override
    public void run(String... args) {
    
    if (System.getenv("GITHUB_ACTIONS") != null || System.console() == null) {
        System.out.println("CI Environment detected. Skipping interactive menu...");
        return; 
    }
    startInteractiveMenu();
    }




    private void startInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        System.out.println("=== Citizen Client Starter (2025) ===");
        
        while (running) {
            try {
                System.out.println("\n--- ΜΕΝΟΥ ΔΙΑΧΕΙΡΙΣΗΣ ΠΟΛΙΤΩΝ ---");
                System.out.println("1. Λίστα Πολιτών");
                System.out.println("2. Εύρεση με ΑΤ");
                System.out.println("3. Εισαγωγή Νέου");
                System.out.println("4. Ενημέρωση (PATCH)");
                System.out.println("5. Διαγραφή (DELETE)");
                System.out.println("Οποιαδήποτε άλλη επιλογή για ΕΞΟΔΟΣ");
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
                        System.out.println("Τερματισμός...");
                        running = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Σφάλμα: " + e.getMessage());
            }
        }
    }

    private void listAll() {
        Citizen[] citizens = restTemplate.getForObject(BASE_URL + "/search", Citizen[].class);
        if (citizens != null) {
            for (Citizen c : citizens) {
                System.out.println(c.getAt() + " - " + c.getFirstName() + " " + c.getLastName());
            }
        }
    }

    private void getByAt(Scanner scanner) {
        System.out.print("Εισάγετε ΑΤ: ");
        String at = scanner.nextLine();
        try {
            Citizen c = restTemplate.getForObject(BASE_URL + "/" + at, Citizen.class);
            if (c != null) System.out.println("Βρέθηκε: " + c.getFirstName() + " " + c.getLastName());
        } catch (Exception e) {
            System.out.println("Ο πολίτης δεν βρέθηκε.");
        }
    }

    private void create(Scanner scanner) {
        Citizen c = new Citizen();
        System.out.print("ΑΤ: "); c.setAt(scanner.nextLine());
        System.out.print("Όνομα: "); c.setFirstName(scanner.nextLine());
        System.out.print("Επίθετο: "); c.setLastName(scanner.nextLine());
        System.out.print("Φύλο: "); c.setGender(scanner.nextLine());
        System.out.print("ΑΦΜ: "); c.setAfm(scanner.nextLine());
        
        restTemplate.postForObject(BASE_URL, c, Citizen.class);
        System.out.println("Επιτυχής εισαγωγή.");
    }

    private void update(Scanner scanner) {
        System.out.print("ΑΤ για ενημέρωση: ");
        String at = scanner.nextLine();
        Citizen update = new Citizen();
        System.out.print("Νέα Διεύθυνση: "); update.setAddress(scanner.nextLine());
        
        restTemplate.patchForObject(BASE_URL + "/" + at, update, String.class);
        System.out.println("Η ενημέρωση ολοκληρώθηκε.");
    }

    private void delete(Scanner scanner) {
        System.out.print("ΑΤ για διαγραφή: ");
        String at = scanner.nextLine();
        restTemplate.delete(BASE_URL + "/" + at);
        System.out.println("Διαγράφηκε επιτυχώς.");
    }
}
