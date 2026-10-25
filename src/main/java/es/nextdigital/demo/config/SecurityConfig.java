package es.nextdigital.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    /** Solo usamos el módulo crypto de Spring Security para hashear el PIN con BCrypt. */
    @Bean
    public PasswordEncoder pinEncoder() {
        return new BCryptPasswordEncoder();
    }
}
