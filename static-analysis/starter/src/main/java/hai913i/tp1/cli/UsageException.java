package hai913i.tp1.cli;

/** A command-line error: the message is shown to the user with the usage text. */
public final class UsageException extends Exception {
  public UsageException(String message) {
    super(message);
  }
}