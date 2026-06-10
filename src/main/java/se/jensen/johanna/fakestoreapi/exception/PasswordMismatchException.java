package se.jensen.johanna.fakestoreapi.exception;

public class PasswordMismatchException extends DomainException {

  public PasswordMismatchException(String message) {
    super(message, ErrorType.PASSWORD_MIS_MATCH);
  }
}
