package takamaka.util;

import java.util.Iterator;
import java.util.stream.Collectors;

import takamaka.lang.Storage;

/**
 * A partial implements of the {@link takamaka.util.ByteArray} interface,
 * containing all methods common to its subclasses.
 */
public abstract class AbstractByteArray extends Storage implements ByteArray {

	@Override
	public String toString() {
		return stream().mapToObj(String::valueOf).collect(Collectors.joining(",", "[", "]"));
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ByteArray && length() == ((ByteArray) other).length()) {
			Iterator<Byte> otherIt = ((ByteArray) other).iterator();
			for (byte b: this)
				if (b != otherIt.next())
					return false;

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int shift = 0;
		int result = 0;
		for (byte b: this) {
			result ^= b << shift;
			shift = (shift + 1) % 24;
		}

		return result;
	}
}