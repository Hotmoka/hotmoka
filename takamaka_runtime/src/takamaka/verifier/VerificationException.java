package takamaka.verifier;

public class VerificationException extends RuntimeException {

	public VerificationException() {}

	public VerificationException(String message) {
		super(message);
	}
}