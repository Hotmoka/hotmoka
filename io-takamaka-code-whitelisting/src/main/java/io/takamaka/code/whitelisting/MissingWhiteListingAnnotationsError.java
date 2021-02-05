package io.takamaka.code.whitelisting;

/**
 * An exception thrown when the white-listing annotations are missing
 * for the version of the verification module of the node.
 */
public class MissingWhiteListingAnnotationsError extends java.lang.Error {
	private static final long serialVersionUID = 563905276281900393L;

	public final int verificationVerification;

	public MissingWhiteListingAnnotationsError(int verificationVerification) {
		super("the white-listing annotations are missing for verification version " + verificationVerification);

		this.verificationVerification = verificationVerification;
	}
}