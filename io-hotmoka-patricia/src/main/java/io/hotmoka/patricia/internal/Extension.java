package io.hotmoka.patricia.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * An extension node of a Patricia tree.
 */
public class Extension extends Node {
	public final static byte SELECTOR = 0;
	private final byte[] sharedNibbles;

	public Extension(byte[] sharedNibbles, byte[] value) {
		super(value);

		this.sharedNibbles = sharedNibbles;
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		oos.write(value);
		oos.write(sharedNibbles);
	}
}