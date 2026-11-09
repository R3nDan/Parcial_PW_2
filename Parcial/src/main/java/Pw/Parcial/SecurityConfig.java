package Pw.Parcial;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {
    private static final RequestMatcher API_REQUEST = request -> request.getServletPath().startsWith("/api/");
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                        .password(u.getPassword()).roles(u.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/medicos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/consultas").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/consultas/*/horario").hasRole("PACIENTE")
                .requestMatchers("/api/consultas").hasAnyRole("ADMINISTRADOR", "MEDICO", "PACIENTE")
                .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/medico/**").hasRole("MEDICO")
                .requestMatchers("/paciente/**").hasRole("PACIENTE")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/acceso-denegado")
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API_REQUEST)
                .defaultAccessDeniedHandlerFor(new AccessDeniedHandlerImpl(), API_REQUEST))
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .httpBasic(Customizer.withDefaults())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
