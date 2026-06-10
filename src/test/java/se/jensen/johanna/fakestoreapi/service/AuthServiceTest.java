package se.jensen.johanna.fakestoreapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import se.jensen.johanna.fakestoreapi.dto.AuthResult;
import se.jensen.johanna.fakestoreapi.dto.LoginRequest;
import se.jensen.johanna.fakestoreapi.dto.RefreshResult;
import se.jensen.johanna.fakestoreapi.dto.RegisterUserRequest;
import se.jensen.johanna.fakestoreapi.exception.PasswordMismatchException;
import se.jensen.johanna.fakestoreapi.model.AppUser;
import se.jensen.johanna.fakestoreapi.model.RefreshToken;
import se.jensen.johanna.fakestoreapi.model.Role;
import se.jensen.johanna.fakestoreapi.security.MyUserDetails;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private TokenService tokenService;

  @Mock
  private RefreshTokenService refreshTokenService;
  @Mock
  private UserService userService;
  @Mock
  private AuthenticationManager authenticationManager;

  private MyUserDetails userDetails;

  private UUID userId;
  private RefreshToken refreshToken;
  private AppUser user;

  @BeforeEach
  void setUp() {

    userId = UUID.randomUUID();
    user = new AppUser(userId, Role.USER, "test@test.com", "hashedPw", null, Instant.now(),
        null);

    userDetails = new MyUserDetails(user);

    refreshToken = new RefreshToken(UUID.randomUUID(), "refreshTokenStr", user, Instant.now(),
        Instant.now().plusSeconds(3600));
  }

  @Test
  void login_ShouldSuccessfullyLogInAndReturnToken() {
    LoginRequest loginRequest = new LoginRequest("test@test.com", "Password1");
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(userDetails);
    when(authenticationManager.authenticate(any())).thenReturn(auth);
    when(tokenService.generateToken(any(), any(), any())).thenReturn("mock-token");
    when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

    AuthResult result = authService.login(loginRequest);

    assertThat(result.loginResponse().userId()).isEqualTo(userId);
    assertThat(result.loginResponse().accessToken()).isNotNull();
    assertThat(result.loginResponse().accessToken()).isEqualTo("mock-token");
    assertThat(result.refreshToken()).isNotNull();
    assertThat(result.refreshToken()).isEqualTo(refreshToken.getToken());
    assertThat(result.loginResponse().role()).isEqualTo(Role.USER);

  }

  @Test
  void register_ShouldSuccessfullyRegisterUserAndLogin() {
    RegisterUserRequest request = new RegisterUserRequest("test@test.com", "Password1",
        "Password1");

    when(userService.createUser(request)).thenReturn(user);
    when(tokenService.generateToken(any(), any(), any())).thenReturn("mock-token");
    when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

    AuthResult result = authService.register(request);

    assertThat(result.loginResponse().userId()).isEqualTo(userId);
    assertThat(result.loginResponse().accessToken()).isNotNull();
    assertThat(result.loginResponse().accessToken()).isEqualTo("mock-token");
    assertThat(result.refreshToken()).isNotNull();
    assertThat(result.refreshToken()).isEqualTo(refreshToken.getToken());
    assertThat(result.loginResponse().role()).isEqualTo(Role.USER);
  }

  @Test
  void register_ShouldThrowExceptionIfPasswordsDoNotMatch() {
    RegisterUserRequest request = new RegisterUserRequest("test@test.com", "Password1",
        "thisDoNotMatch2");

    assertThrows(PasswordMismatchException.class, () -> authService.register(request));
  }

  @Test
  void refresh_ShouldRefreshAndReturnNewAccessToken() {
    String oldRefreshToken = refreshToken.getToken();
    RefreshToken newRefreshToken = new RefreshToken(UUID.randomUUID(), "newRefreshToken", user,
        Instant.now().plusSeconds(1000), Instant.now().plusSeconds(3600));
    when(refreshTokenService.findByToken(any())).thenReturn(refreshToken);
    when(refreshTokenService.rotateRefreshToken(any())).thenReturn(newRefreshToken);
    when(tokenService.generateToken(any(), any(), any())).thenReturn("newAccessToken");

    RefreshResult result = authService.refresh(oldRefreshToken);
    assertThat(result.refreshResponse().accessToken()).isNotNull();
    assertThat(result.refreshResponse().accessToken()).isEqualTo("newAccessToken");
    assertThat(result.refreshToken()).isNotNull();
    assertThat(result.refreshToken()).isEqualTo(newRefreshToken.getToken());

  }


}