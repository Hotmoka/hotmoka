package io.hotmoka.beans.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

public class UnmarshallingUtils {

	public static BigInteger unmarshallBigInteger(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		return (BigInteger) ois.readObject();
	}
}