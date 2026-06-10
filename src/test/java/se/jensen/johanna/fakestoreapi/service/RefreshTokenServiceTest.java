package se.jensen.johanna.fakestoreapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.jensen.johanna.fakestoreapi.exception.DomainStateException;
import se.jensen.johanna.fakestoreapi.exception.TokenExpirationException;
import se.jensen.johanna.fakestoreapi.model.AppUser;
import se.jensen.johanna.fakestoreapi.model.RefreshToken;
import se.jensen.johanna.fakestoreapi.model.Role;
import se.jensen.johanna.fakestoreapi.repository.RefreshTokenRepository;
import se.jensen.johanna.fakestoreapi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @InjectMocks
  private RefreshTokenService refreshTokenService;
  @Mock
  private RefreshTokenRepository refreshTokenRepository;
  @Mock
  private UserRepository userRepository;

  private RefreshToken refreshToken;
  private AppUser user;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = new AppUser(userId, Role.USER, "test@test.com", "hashedPw", null, Instant.now(), null);
    refreshToken = new RefreshToken(UUID.randomUUID(), "refreshToken", user,
        Instant.now().plusSeconds(1000), Instant.now());

  }

  @Test
  void createRefreshToken_ShouldCreateNewRefreshToken() {

    ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    when(userRepository.findById(any())).thenReturn(Optional.of(user));
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(
        inv -> inv.getArgument(0));

    RefreshToken result = refreshTokenService.createRefreshToken(userId);

    verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
    assertThat(result.getToken()).isNotNull();
    assertEquals(userId, result.getUser().getUserId());
    verify(refreshTokenRepository).deleteByUser(user);

  }

  @Test
  void findByToken_ShouldFindRefreshToken() {
    when(refreshTokenRepository.findByToken(refreshToken.getToken())).thenReturn(
        Optional.of(refreshToken));

    RefreshToken result = refreshTokenService.findByToken(refreshToken.getToken());
    assertThat(result).isNotNull();
    assertEquals(refreshToken.getToken(), result.getToken());
  }

  @Test
  void findByToken_ShouldThrowExceptionIfTokenNotFound() {
    when(refreshTokenRepository.findByToken(refreshToken.getToken())).thenReturn(Optional.empty());
    assertThrows(DomainStateException.class,
        () -> refreshTokenService.findByToken(refreshToken.getToken()));
  }

  @Test
  void verifyExpiration_shouldThrowExceptionIfTokenExpired() {
    Instant now = Instant.now();
    Instant expired = now.minusSeconds(1000);
    RefreshToken expiredToken = new RefreshToken(UUID.randomUUID(), "expiredToken", user,
        expired, now);

    assertThrows(TokenExpirationException.class,
        () -> refreshTokenService.verifyExpiration(expiredToken));
  }

  @Test
  void verifyExpiration_shouldNotThrowExceptionIfTokenNotExpired() {
    assertDoesNotThrow(() -> refreshTokenService.verifyExpiration(refreshToken));

  }


}