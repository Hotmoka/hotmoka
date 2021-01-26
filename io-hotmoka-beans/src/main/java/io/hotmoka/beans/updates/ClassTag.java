package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;

/**
 * An update that states that an object belongs to a given class.
 * It is stored in blockchain by the transaction that created the
 * object and is not modified later anymore.
 */
@Immutable
public final class ClassTag extends Update {
	final static byte SELECTOR = 0;

	/**
	 * The name of the class of the object.
	 */
	public final String className;

	/**
	 * The transaction that installed the jar from which the class was resolved.
	 */
	public final TransactionReference jar;

	/**
	 * Builds an update for the class tag of an object.
	 * 
	 * @param object the storage reference of the object whose class name is set
	 * @param className the name of the class of the object
	 * @param jar the transaction that installed the jar from which the class was resolved
	 */
	public ClassTag(StorageReference object, String className, TransactionReference jar) {
		super(object);

		this.className = className;
		this.jar = jar;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassTag && super.equals(other)
			&& ((ClassTag) other).className.equals(className)
			&& ((ClassTag) other).jar.equals(jar);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ className.hashCode() ^ jar.hashCode();
	}

	@Override
	public String toString() {
		return "<" + object + ".class|" + className + "|@" + jar + ">";
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;

		diff = className.compareTo(((ClassTag) other).className);
		if (diff != 0)
			return diff;
		else
			return jar.compareTo(((ClassTag) other).jar);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(className)).add(gasCostModel.storageCostOf(jar));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		super.into(context);
		context.oos.writeUTF(className);
		jar.into(context);
	}

	@Override
	public boolean sameProperty(Update other) {
		return other instanceof ClassTag;
	}
}