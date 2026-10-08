package za.ac.mycput;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * EduLink REST API — http://localhost:8080/api
 * API documentation — http://localhost:8080/swagger-ui.html
 */
// Authentication is JWT-based (see security package), so Spring's default in-memory user is not needed
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EduLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduLinkApplication.class, args);
    }
}
