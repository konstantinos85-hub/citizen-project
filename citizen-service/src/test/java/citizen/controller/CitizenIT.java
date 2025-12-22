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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CitizenIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
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

        given()
            .contentType(ContentType.JSON)
            .body(citizen)
        .when()
            .post()
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/XY100000")
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Constantinos"));
    }
}
