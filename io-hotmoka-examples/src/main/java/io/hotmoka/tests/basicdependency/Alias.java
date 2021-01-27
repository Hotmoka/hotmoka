package io.hotmoka.tests.basicdependency;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

// a test on equality of deserialized values
public class Alias extends Storage {

	@View
	public boolean test(Alias a1, Alias a2) {
		return a1 == a2;
	}

	@View
	public boolean test(String s1, String s2) {
		return s1 == s2;
	}

	@View
	public boolean test(BigInteger bi1, BigInteger bi2) {
		return bi1 == bi2;
	}
}