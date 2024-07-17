package io.hotmoka.node.local.internal;

import java.util.Arrays;

import io.hotmoka.crypto.Hex;
import io.hotmoka.node.local.api.StateId;

/**
 * Implementation of the identifier of the state of a trie-based store.
 */
public class StateIdImpl implements StateId {
	private final byte[] bytes;

	/**
	 * Creates an identifier represented by the given bytes.
	 * 
	 * @param id the bytes of the identifier
	 */
	public StateIdImpl(byte[] id) {
		this.bytes = id;
	}

	@Override
	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StateId si && Arrays.equals(bytes, si.getBytes());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	@Override
	public String toString() {
		return Hex.toHexString(bytes);
	}

	@Override
	public int compareTo(StateId other) {
		if (other instanceof StateIdImpl sii)
			return Arrays.compare(bytes, sii.bytes); // optimization
		else
			return Arrays.compare(bytes, other.getBytes());
	}
}