package za.ac.mycput.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * Log in via POST /api/auth/login, copy the token, click "Authorize" and paste it to try protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eduLinkOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduLink API")
                        .version("2.0.0")
                        .description("REST API connecting students with employers for jobs, internships and graduate programmes."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token returned by POST /api/auth/login")));
    }
}
