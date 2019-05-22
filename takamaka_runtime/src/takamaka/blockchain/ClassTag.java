package takamaka.blockchain;

import takamaka.blockchain.values.StorageReference;
import takamaka.lang.Immutable;

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
	 * Builds an update for the class tag of an object.
	 * 
	 * @param object the storage reference of the object whose class name is set
	 * @param className the name of the class of the object
	 */
	public ClassTag(StorageReference object, String className) {
		super(object);

		this.className = className;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassTag && super.equals(other)
			&& ((ClassTag) other).className.equals(className);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ className.hashCode();
	}

	@Override
	public String toString() {
		return "<" + object + ".class|" + className + ">";
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return className.compareTo(((ClassTag) other).className);
	}

	@Override
	public ClassTag contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new ClassTag(objectContextualized, className);
		else
			return this;
	}
}