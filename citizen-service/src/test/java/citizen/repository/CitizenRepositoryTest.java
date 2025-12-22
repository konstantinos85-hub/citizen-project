package citizen.repository;

import citizen.model.Citizen;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // Ρυθμίζει αυτόματα μια in-memory βάση για δοκιμές ORM
class CitizenRepositoryTest {

    @Autowired
    private CitizenRepository repository;

    @Test
    void testSaveAndFindCitizen() {
        // Δημιουργία και αποθήκευση
        Citizen citizen = new Citizen();
        citizen.setAt("BT999999");
        citizen.setFirstName("Μαρία");
        citizen.setLastName("Δημητρίου");
        citizen.setGender("Γυναίκα");
        
        repository.save(citizen);

        // Έλεγχος ORM (Ανάκτηση από τη βάση)
        Citizen found = repository.findById("BT999999").orElse(null);
        
        assertThat(found).isNotNull();
        assertThat(found.getFirstName()).isEqualTo("Μαρία");
        assertThat(found.getLastName()).isEqualTo("Δημητρίου");
    }
}
