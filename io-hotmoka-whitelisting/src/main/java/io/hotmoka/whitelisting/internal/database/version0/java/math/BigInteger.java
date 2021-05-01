/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.whitelisting.internal.database.version0.java.math;

public abstract class BigInteger {
	public static java.math.BigInteger ONE;
	public static java.math.BigInteger TWO;
	public static java.math.BigInteger TEN;
	public static java.math.BigInteger ZERO;

	public BigInteger(java.lang.String val) {}
	public BigInteger(java.lang.String val, int radix) {}

	public abstract int signum();
	public abstract java.math.BigInteger valueOf(long val);
	public abstract boolean equals(java.lang.Object other);
	public abstract int hashCode();
	public abstract java.lang.String toString();
	public abstract java.lang.String toString(int radix);
	public abstract byte[] toByteArray();
	public abstract int compareTo(java.math.BigInteger other);
	public abstract java.math.BigInteger add(java.math.BigInteger val);
	public abstract java.math.BigInteger subtract(java.math.BigInteger val);
	public abstract java.math.BigInteger multiply(java.math.BigInteger val);
	public abstract java.math.BigInteger divide(java.math.BigInteger val);
	public abstract java.math.BigInteger[] divideAndRemainder(java.math.BigInteger divisor);
	public abstract java.math.BigInteger mod(java.math.BigInteger m);
	public abstract java.math.BigInteger pow(int exponent);
	public abstract java.math.BigInteger max(java.math.BigInteger val);
	public abstract java.math.BigInteger min(java.math.BigInteger val);
}