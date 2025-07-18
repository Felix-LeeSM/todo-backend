package rest.felix.back.common.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpServletResponse.SC_FORBIDDEN);
    body.put("error", "Forbidden");
    body.put("message", "허가되지 않은 접근입니다.");
    body.put("path", request.getRequestURI());

    objectMapper.writeValue(response.getWriter(), body);
  }
}
