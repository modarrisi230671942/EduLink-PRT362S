package za.ac.mycput.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import za.ac.mycput.exception.GlobalExceptionHandler;
import za.ac.mycput.repository.UserRepository;

import java.io.IOException;
import java.util.List;

/**
 * Security rules for the whole API.
 * <ul>
 *   <li>Stateless: every request carries a JWT; there are no server sessions (so CSRF protection is not needed).</li>
 *   <li>URL rules below give coarse role checks; controllers add ownership checks
 *       (e.g. a company may only edit its own jobs).</li>
 *   <li>Passwords are hashed with BCrypt.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserRepository userRepository,
                                                   ObjectMapper objectMapper) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/*", "/api/public/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                // WebSocket handshake: authentication happens in the STOMP CONNECT frame (WebSocketAuthInterceptor)
                .requestMatchers("/ws", "/ws/**").permitAll()
                // Role-restricted actions — checked here, before request bodies are even validated,
                // so a user without the role cannot probe the API's validation rules
                .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("COMPANY")
                .requestMatchers(HttpMethod.PUT, "/api/jobs/*").hasRole("COMPANY")
                .requestMatchers(HttpMethod.PATCH, "/api/jobs/*/status").hasRole("COMPANY")
                .requestMatchers(HttpMethod.DELETE, "/api/jobs/*").hasAnyRole("COMPANY", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/applications").hasRole("STUDENT")
                .requestMatchers(HttpMethod.DELETE, "/api/applications/*").hasRole("STUDENT")
                .requestMatchers(HttpMethod.PATCH, "/api/applications/*/status").hasRole("COMPANY")
                .requestMatchers(HttpMethod.POST, "/api/applications/*/interview").hasRole("COMPANY")
                .requestMatchers(HttpMethod.POST, "/api/interviews/*/confirm").hasRole("STUDENT")
                // Role-restricted areas
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/students/**").hasRole("STUDENT")
                .requestMatchers("/api/companies/**").hasRole("COMPANY")
                // Everything else needs a logged-in user (finer checks via @PreAuthorize)
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> writeError(response, objectMapper,
                        HttpStatus.UNAUTHORIZED, "Please log in to continue.", request.getRequestURI()))
                .accessDeniedHandler((request, response, ex) -> writeError(response, objectMapper,
                        HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request.getRequestURI())))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Only the configured frontend origins may call the API from a browser. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${edulink.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper,
                                   HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), GlobalExceptionHandler.body(status, message, path, null));
    }
}
