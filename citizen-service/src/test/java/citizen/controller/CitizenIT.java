package citizen.controller;

import citizen.model.Citizen;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
                properties = "spring.main.banner-mode=off") // Απενεργοποίηση banner για καθαρά logs
@ActiveProfiles("test") // Ενεργοποίηση του test profile
public class CitizenIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        // Αν το API σας είναι απευθείας στο /citizens, αφήστε το κενό ή βάλτε το prefix
        RestAssured.basePath = "/api/citizens"; 
    }

    @Test
    public void testCreateAndGetCitizen() {
        Citizen citizen = new Citizen();
        citizen.setAt("XY100000");
        citizen.setFirstName("Constantinos");
        citizen.setLastName("Kouyouris");
        citizen.setGender("Male");
        citizen.setAfm("123456789");

        // 1. Δοκιμή POST (Δημιουργία)
        given()
            .contentType(ContentType.JSON)
            .body(citizen)
        .when()
            .post()
        .then()
            .statusCode(anyOf(is(200), is(201))); // Δέχεται 200 OK ή 201 Created

        // 2. Δοκιμή GET (Ανάκτηση βάσει AT)
        given()
        .when()
            .get("/XY100000")
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Constantinos"))
            .body("at", equalTo("XY100000"));
    }
}
