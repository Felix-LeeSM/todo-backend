package rest.felix.back.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import rest.felix.back.user.service.UserService;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserService userService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Optional.ofNullable(request.getCookies())
        .flatMap(
            cookies ->
                Arrays.stream(cookies)
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .findFirst())
        .map(Cookie::getValue)
        .filter(jwtTokenProvider::validateToken)
        .map(jwtTokenProvider::getAuthUserFromToken)
        .filter(authUser -> userService.existsById(authUser.getUserId()))
        .ifPresent(
            authUser ->
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            authUser, null, new ArrayList<>())));

    filterChain.doFilter(request, response);
  }
}
