package takamaka.blockchain;

import java.math.BigInteger;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.values.StorageReference;

/**
 * An update that states that an object belongs to a given class.
 * It is stored in blockchain by the transaction that created the
 * object and is not modified later anymore.
 */
@Immutable
public final class ClassTag extends Update {

	private static final long serialVersionUID = 7597397926867306935L;

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
	public ClassTag contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new ClassTag(objectContextualized, className, jar);
		else
			return this;
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.storageCostOf(className)).add(jar.size());
	}
}