package io.takamaka.code.verification;

/**
 * An exception thrown when the verification version of the node
 * is not supported by the verification module of the node.
 */
public class UnsupportedVerificationVersionError extends java.lang.Error {
	private static final long serialVersionUID = -1232455923178336022L;
	public final int verificationVerification;

	public UnsupportedVerificationVersionError(int verificationVerification) {
		super("the verification module does not support version " + verificationVerification);

		this.verificationVerification = verificationVerification;
	}
}