package citizen.client;

import citizen.model.Citizen;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import java.util.Scanner;

@Component
public class CitizenClient implements CommandLineRunner {

    private final String BASE_URL = "http://localhost:8089/api/citizens";
    private final RestTemplate restTemplate;
    private final Scanner scanner = new Scanner(System.in);

    public CitizenClient() {
        // Ρύθμιση για να υποστηρίζει το RestTemplate την εντολή PATCH το 2025
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Override
    public void run(String... args) {
        boolean running = true;

        while (running) {
            System.out.println("\n======================================");
            System.out.println("   ΜΕΝΟΥ ΔΙΑΧΕΙΡΙΣΗΣ ΠΟΛΙΤΩΝ (2025)   ");
            System.out.println("======================================");
            System.out.println("1. Προβολή όλων των πολιτών");
            System.out.println("2. Αναζήτηση πολίτη με ΑΤ");
            System.out.println("3. Εισαγωγή νέου πολίτη (POST)");
            System.out.println("4. Ενημέρωση διεύθυνσης/ΑΦΜ (PATCH)");
            System.out.println("5. Διαγραφή πολίτη (DELETE)");
            System.out.println("--------------------------------------");
            System.out.println("Οποιαδήποτε άλλη επιλογή -> Τερματισμός");
            System.out.print("Επιλογή: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1" -> listAll();
                    case "2" -> getByAt();
                    case "3" -> create();
                    case "4" -> update();
                    case "5" -> delete();
                    default -> {
                        System.out.println("Μη έγκυρη επιλογή. Τερματισμός...");
                        running = false;
                        System.exit(0); // Κλείνει την εφαρμογή
                    }
                }
            } catch (Exception e) {
                System.out.println("Σφάλμα κατά την εκτέλεση: " + e.getMessage());
            }
        }
    }

    private void listAll() {
        Citizen[] citizens = restTemplate.getForObject(BASE_URL + "/search", Citizen[].class);
        if (citizens != null && citizens.length > 0) {
            for (Citizen c : citizens) {
                System.out.println(c.getAt() + " | " + c.getFirstName() + " " + c.getLastName() + " | ΑΦΜ: " + c.getAfm());
            }
        } else {
            System.out.println("Δεν βρέθηκαν πολίτες.");
        }
    }

    private void getByAt() {
        System.out.print("Δώστε ΑΤ: ");
        String at = scanner.nextLine();
        try {
            Citizen c = restTemplate.getForObject(BASE_URL + "/" + at, Citizen.class);
            System.out.println("Στοιχεία: " + c.getFirstName() + " " + c.getLastName() + ", Διεύθυνση: " + c.getAddress());
        } catch (Exception e) {
            System.out.println("Ο πολίτης με ΑΤ " + at + " δεν βρέθηκε.");
        }
    }

    private void create() {
        Citizen c = new Citizen();
        System.out.print("ΑΤ: "); c.setAt(scanner.nextLine());
        System.out.print("Όνομα: "); c.setFirstName(scanner.nextLine());
        System.out.print("Επίθετο: "); c.setLastName(scanner.nextLine());
        System.out.print("Φύλο: "); c.setGender(scanner.nextLine());
        System.out.print("Ημ. Γέννησης (ΧΧ-ΥΥ-ΚΚΚΚ): "); c.setBirthDate(scanner.nextLine());
        System.out.print("ΑΦΜ: "); c.setAfm(scanner.nextLine());
        System.out.print("Διεύθυνση: "); c.setAddress(scanner.nextLine());

        restTemplate.postForObject(BASE_URL, c, Citizen.class);
        System.out.println("Η εγγραφή ολοκληρώθηκε επιτυχώς!");
    }

    private void update() {
        System.out.print("Δώστε ΑΤ για ενημέρωση: ");
        String at = scanner.nextLine();
        Citizen updateData = new Citizen();
        System.out.print("Νέο ΑΦΜ (ή κενό): "); updateData.setAfm(scanner.nextLine());
        System.out.print("Νέα Διεύθυνση (ή κενό): "); updateData.setAddress(scanner.nextLine());

        restTemplate.patchForObject(BASE_URL + "/" + at, updateData, String.class);
        System.out.println("Η ενημέρωση ολοκληρώθηκε.");
    }

    private void delete() {
        System.out.print("Δώστε ΑΤ για διαγραφή: ");
        String at = scanner.nextLine();
        restTemplate.delete(BASE_URL + "/" + at);
        System.out.println("Ο πολίτης διαγράφηκε.");
    }
}
