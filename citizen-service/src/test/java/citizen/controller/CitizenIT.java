package citizen.controller;

import citizen.model.Citizen;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration Test (IT) για την RESTful υπηρεσία Citizen.
 * Χρησιμοποιεί το Rest-Assured για την επαλήθευση των HTTP endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CitizenIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        // Ρύθμιση της θύρας και του base path για το Rest-Assured
        RestAssured.port = port;
        RestAssured.basePath = "/api/citizens";
    }

    @Test
    public void testCreateAndGetCitizen() {
        // Δημιουργία αντικειμένου για δοκιμή
        Citizen citizen = new Citizen();
        citizen.setAt("XY100000");
        citizen.setFirstName("Constantinos");
        citizen.setLastName("Kouyouris");
        citizen.setGender("Male");
        citizen.setAfm("123456789");

        // 1. Δοκιμή POST: Έλεγχος εισαγωγής νέου πολίτη
        given()
            .contentType(ContentType.JSON)
            .body(citizen)
        .when()
            .post()
        .then()
            .statusCode(200) // OK
            .body("at", equalTo("XY100000"))
            .body("firstName", equalTo("Constantinos"));

        // 2. Δοκιμή GET: Έλεγχος ανάκτησης με βάση τον ΑΤ
        given()
        .when()
            .get("/XY100000")
        .then()
            .statusCode(200)
            .body("lastName", equalTo("Kouyouris"))
            .body("afm", equalTo("123456789"));
    }

    @Test
    public void testGetNonExistingCitizen() {
        // Δοκιμή αναζήτησης ΑΤ που δεν υπάρχει
        given()
        .when()
            .get("/NOTEXIST")
        .then()
            .statusCode(404); // Not Found
    }

    @Test
    public void testDeleteCitizen() {
        // Προετοιμασία: Εισαγωγή ενός πολίτη προς διαγραφή
        Citizen c = new Citizen();
        c.setAt("ZZ999999");
        c.setFirstName("Delete");
        c.setLastName("Me");
        c.setGender("Other");
        
        given().contentType(ContentType.JSON).body(c).post();

        // Δοκιμή DELETE
        given()
        .when()
            .delete("/ZZ999999")
        .then()
            .statusCode(200);

        // Επιβεβαίωση διαγραφής (GET -> 404)
        given()
        .when()
            .get("/ZZ999999")
        .then()
            .statusCode(404);
    }
}
