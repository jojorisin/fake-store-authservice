package se.jensen.johanna.fakestoreapi.controller;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/auth")
@RequiredArgsConstructor
public class JwkSetController {

  private final JWKSource<SecurityContext> jwkSource;

  @GetMapping("/.well-known/jwks.json")
  public Map<String, Object> keys() throws Exception {
    JWKSelector jwkSelector = new JWKSelector(new JWKMatcher.Builder().build());
    return new JWKSet(jwkSource.get(jwkSelector, null)).toJSONObject(true);
  }

}
