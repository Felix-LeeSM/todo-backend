package rest.felix.back.common.config.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import rest.felix.back.common.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(customAuthenticationEntryPoint))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/v1/user")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/user/me")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/user/token/access-token")
                    .permitAll()
                    .requestMatchers(
                        request -> {
                          String ip = request.getRemoteAddr();
                          return isLocalAddress(ip)
                              && (request.getRequestURI().startsWith("/swagger-ui")
                                  || request.getRequestURI().startsWith("/v3/api-docs"));
                        })
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private boolean isLocalAddress(String ip) {
    return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1"); // IPv4 and IPv6 localhost
  }
}
