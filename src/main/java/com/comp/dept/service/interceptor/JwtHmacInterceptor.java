package com.comp.dept.service.interceptor;


import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.auth0.jwt.JWT.require;
import static com.auth0.jwt.algorithms.Algorithm.HMAC256;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@Slf4j
@Setter
public class JwtHmacInterceptor extends HandlerInterceptorAdapter {

  private static final String BEARER = "Bearer" ;
  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private HmacRepository hmacRepository;

  @Autowired
  private AppConfiguration appConfig;

  private static final String TOKEN_MATCHER = "^Bearer\\s.+";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    String errorMsg;
    try {
      String bearerToken = request.getHeader(AUTHORIZATION);
      String jwtAuthToken = bearerToken != null && bearerToken.matches(TOKEN_MATCHER)
              ? bearerToken.replace(BEARER, "").trim() : null;
      JwtPayloadToken jwtPayloadToken =
          objectMapper.readValue(
              new String(
                  decodeBase64(JWT.decode(jwtAuthToken).getPayload()),
                  StandardCharsets.UTF_8.name()),
              JwtPayloadToken.class);
      String secret = hmacRepository.getHmacMap().get(jwtPayloadToken.getSub());
      if (secret != null
          && (Instant.now().getEpochSecond() >= jwtPayloadToken.getIat())
          && ((Instant.now().getEpochSecond() - jwtPayloadToken.getIat())
              <= appConfig.getValidTokenWindow())){
        require(HMAC256(secret)).build().verify(jwtAuthToken);
        request.setAttribute(HttpHeaders.USER_AGENT, jwtPayloadToken.getSub());
        return true;
      } else {
        errorMsg = "unauthorized user / expired token";
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      errorMsg = "invalid token";
    }
    response.sendError(SC_UNAUTHORIZED, errorMsg);

    return false;
  }
}
