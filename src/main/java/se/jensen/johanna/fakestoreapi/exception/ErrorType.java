package se.jensen.johanna.fakestoreapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
  ILLEGAL_STATE(HttpStatus.INTERNAL_SERVER_ERROR),
  PASSWORD_MIS_MATCH(HttpStatus.BAD_REQUEST);

  private final HttpStatus status;

  ErrorType(HttpStatus status) {
    this.status = status;
  }


}
