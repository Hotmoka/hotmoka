package io.takamaka.code.whitelisting;

/**
 * A predicate for a value, that must satisfy some property if used
 * in a white-listed method or constructor.
 */
public interface WhiteListingPredicate {

	/**
	 * Checks if the given value satisfies the condition expressed by this predicate.
	 * 
	 * @param value the value to check
	 * @param wizard the object that can be used to access white-listing annotations about the library
	 * @return true if and only if {@code value} satisfies the conditions expressed by this predicate
	 */
	boolean test(Object value, WhiteListingWizard wizard);

	/**
	 * Yields the message to report if the predicate fails.
	 * 
	 * @param methodName the name of the method or constructor whose parameter is checked
	 * @return the message
	 */
	String messageIfFailed(String methodName);
}