package com.mtc.mutuaConseil.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class WebAdapterConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Désactive CSRF explicitement
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Autorise toutes les requêtes (à adapter selon vos besoins)
                )
            .cors(cors -> cors.configurationSource(corsConfigurationSource())); // Configure CORS
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200",
                                                "http://localhost:5173",
                                                "http://localhost:5174",
                                                "http://192.168.1.39:4200")); // Origines autorisées
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH")); // Méthodes autorisées
        configuration.setAllowedHeaders(List.of("*")); // En-têtes autorisés
        configuration.setAllowCredentials(true); // Autorise les cookies
        configuration.setMaxAge(3600L); // Durée de vie de la pré-requête CORS (en secondes)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Applique la configuration à tous les endpoints

        return source;
    }
}
