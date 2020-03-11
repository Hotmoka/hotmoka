package io.takamaka.code.whitelisting;

import java.util.function.Predicate;

/**
 * A predicate for a value, that must satisfy some property if used
 * in a white-listed method or constructor.
 */
public interface WhiteListingPredicate extends Predicate<Object> {

	/**
	 * Yields the message to report if the predicate fails.
	 * 
	 * @param methodName the name of the method or constructor whose parameter is checked
	 * @return the message
	 */
	public String messageIfFailed(String methodName);
}