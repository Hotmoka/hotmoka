package io.hotmoka.beans.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

public class MarshallingUtils {

	public static void marshal(BigInteger bi, ObjectOutputStream oos) throws IOException {
		oos.writeObject(bi);
	}
}
