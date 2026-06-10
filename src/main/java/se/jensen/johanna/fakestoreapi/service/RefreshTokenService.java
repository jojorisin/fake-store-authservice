package se.jensen.johanna.fakestoreapi.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.jensen.johanna.fakestoreapi.exception.DomainStateException;
import se.jensen.johanna.fakestoreapi.exception.TokenExpirationException;
import se.jensen.johanna.fakestoreapi.model.AppUser;
import se.jensen.johanna.fakestoreapi.model.RefreshToken;
import se.jensen.johanna.fakestoreapi.repository.RefreshTokenRepository;
import se.jensen.johanna.fakestoreapi.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

  @Value("${app.cookie.refresh-expiration-seconds}")
  private long refreshExpirationSeconds;

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public RefreshToken createRefreshToken(UUID userId) {
    log.info("Attempting to create refresh token for user: {}", userId);
    AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.error("Refresh token creation failed for user: {}. User not found", userId);
          return new DomainStateException("Unexpected error. User not found");
        });
    refreshTokenRepository.deleteByUser(user);
    refreshTokenRepository.flush();
    RefreshToken newRefreshToken = RefreshToken.create(user, refreshExpirationSeconds);
    log.info("Refresh token created for user: {}", user.getEmail());
    return refreshTokenRepository.save(newRefreshToken);
  }

  public RefreshToken findByToken(String token) {
    return refreshTokenRepository.findByToken(token)
        .orElseThrow(() -> {
          log.error("Refresh token not found for token: {}", token);
          return new DomainStateException("Unexpected error. Refresh token not found");
        });
  }

  @Transactional
  public void verifyExpiration(RefreshToken refreshToken) {
    if (refreshToken.isExpired()) {
      log.info("Refresh token expired for user: {}", refreshToken.getUser().getEmail());
      deleteRefreshToken(refreshToken);
      throw new TokenExpirationException("Session has expired. Please login again.");
    }
    log.info("Refresh token is valid for user: {}", refreshToken.getUser().getEmail());

  }

  @Transactional
  public RefreshToken rotateRefreshToken(RefreshToken oldRefreshToken) {
    log.info("Attempting to rotate refresh token for user: {}",
        oldRefreshToken.getUser().getEmail());
    AppUser user = oldRefreshToken.getUser();
    deleteRefreshToken(oldRefreshToken);
    refreshTokenRepository.flush();
    RefreshToken newRefreshToken = RefreshToken.create(user, refreshExpirationSeconds);
    refreshTokenRepository.save(newRefreshToken);
    log.info("Refresh token rotated for user: {}", user.getEmail());
    return newRefreshToken;
  }

  @Transactional
  public void deleteRefreshToken(RefreshToken refreshToken) {
    log.info("Deleting refresh token for user: {}", refreshToken.getUser().getEmail());
    refreshTokenRepository.delete(refreshToken);
  }

}
