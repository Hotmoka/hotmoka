package io.hotmoka.beans;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * A context used during object marshalling into bytes.
 */
public class MarshallingContext {
	public final ObjectOutputStream oos;
	private final Map<BigInteger, BigInteger> memory = new HashMap<>();

	public MarshallingContext(ObjectOutputStream oos) {
		this.oos = oos;
	}

	/**
	 * Writes the given string into the output stream. It uses a memory
	 * to avoid repeated writing of the same string: the second write
	 * will refer to the first one.
	 * 
	 * @param s the string to write
	 * @throws IOException if the string could not be written
	 */
	public void writeObject(String s) throws IOException {
		oos.writeObject(s.intern());
	}

	/**
	 * Writes the given big integer into the output stream. It uses a memory
	 * to avoid repeated writing of the same big integer: the second write
	 * will refer to the first one.
	 * 
	 * @param bi the big integer to write
	 * @throws IOException if the big integer could not be written
	 */
	public void writeObject(BigInteger bi) throws IOException {
		oos.writeObject(memory.computeIfAbsent(bi, _bi -> _bi));
	}
}