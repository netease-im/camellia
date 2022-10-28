package com.netease.nim.camellia.dashboard.samples.security.provider;

import com.netease.nim.camellia.dashboard.samples.security.model.ApiKeyAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

  @Value("${camellia-dashboard.security.secure-token}")
  private String secureToken;

  @Value("${camellia-dashboard.security.enabled}")
  private boolean enabledSecurity;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String apiKey = (String) authentication.getPrincipal();
    if (!enabledSecurity) {
      return new ApiKeyAuthenticationToken(apiKey, true);
    }
    if (ObjectUtils.isEmpty(apiKey)) {
      throw new InsufficientAuthenticationException("No API key in request");
    }
    if (secureToken.equals(apiKey)) {
      return new ApiKeyAuthenticationToken(apiKey, true);
    }
    throw new BadCredentialsException("API Key is invalid");
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
