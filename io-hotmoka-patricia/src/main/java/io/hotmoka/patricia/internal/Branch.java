package io.hotmoka.patricia.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * A branch node of a Patricia tree.
 */
public class Branch extends Node {
	public final static byte SELECTOR = 1;

	/**
	 * The branching children of the node. If the nth child is missing,
	 * the array will hold null for it.
	 */
	private final byte[][] children;

	public Branch(byte[][] children, byte[] value) {
		super(value);

		this.children = children;
	}

	private short selector() {
		short result = 0;

		for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
			if (children[pos] != null)
				result |= bit;

		return result;
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		oos.write(value);
		oos.writeShort(selector());

		for (byte[] child: children)
			if (child != null)
				oos.write(child);
	}
}