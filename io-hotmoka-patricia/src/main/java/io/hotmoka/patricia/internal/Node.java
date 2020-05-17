package io.hotmoka.patricia.internal;

import java.io.IOException;
import java.io.ObjectInputStream;

import io.hotmoka.beans.Marshallable;

/**
 * A node of a Patricia tree.
 */
public abstract class Node extends Marshallable {
	protected final byte[] value;

	protected Node(byte[] value) {
		this.value = value;
	}

	/**
	 * Factory method that unmarshals a node from the given stream.
	 * 
	 * @param ois the stream
	 * @param hashSize the number of bytes in a hash used as value or as reference to another node
	 * @return the node
	 * @throws IOException if the node could not be unmarshalled
	 * @throws ClassNotFoundException if the node could not be unmarshalled
	 */
	public static Node from(ObjectInputStream ois, int hashSize) throws IOException, ClassNotFoundException {
		byte kind = ois.readByte();

		if (kind == Extension.SELECTOR) {
			byte[] value = new byte[hashSize];
			if (hashSize != ois.readNBytes(value, 0, hashSize))
				throw new IOException("hash length mismatch in Patricia node");

			byte[] sharedNibbles = ois.readAllBytes();

			return new Extension(sharedNibbles, value);
		}
		else if (kind == Branch.SELECTOR) {
			byte[] value = new byte[hashSize];
			if (hashSize != ois.readNBytes(value, 0, hashSize))
				throw new IOException("hash length mismatch in Patricia node");

			short selector = ois.readShort();
			byte[][] children = new byte[16][];
			for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
				if ((selector & bit) != 0) {
					children[pos] = new byte[hashSize];
					if (hashSize != ois.readNBytes(children[pos], 0, hashSize))
						throw new IOException("hash length mismatch in Patricia node");
				}

			return new Branch(children, value);
		}
		else
			throw new IOException("unexpected Patrica node kind: " + kind);
	}
}