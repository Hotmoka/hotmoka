package takamaka.blockchain;

import java.io.Serializable;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Immutable;

/**
 * The signature of a field of a class.
 */
@Immutable
public final class FieldSignature implements Serializable, Comparable<FieldSignature> {

	private static final long serialVersionUID = -233403674197930650L;

	/**
	 * The field that holds the balance in externally owned accounts.
	 */
	public final static FieldSignature BALANCE_FIELD = new FieldSignature(ClassType.CONTRACT, "balance", ClassType.BIG_INTEGER);

	/**
	 * The class of the field.
	 */
	public final ClassType definingClass;

	/**
	 * The name of the field.
	 */
	public final String name;

	/**
	 * The type of the field.
	 */
	public final StorageType type;

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignature(ClassType definingClass, String name, StorageType type) {
		this.definingClass = definingClass;
		this.name = name;
		this.type = type;
	}

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the name of the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignature(String definingClass, String name, StorageType type) {
		this(new ClassType(definingClass), name, type);
	}

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the name of the class where the field is defined
	 * @param name the name of the field
	 * @param className the name of the type (class) of the field
	 */
	public FieldSignature(String definingClass, String name, String className) {
		this(new ClassType(definingClass), name, new ClassType(className));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldSignature && ((FieldSignature) other).definingClass.equals(definingClass)
			&& ((FieldSignature) other).name.equals(name) && ((FieldSignature) other).type.equals(type);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ name.hashCode() ^ type.hashCode();
	}

	@Override
	public String toString() {
		return definingClass + "." + name + ":" + type;
	}

	@Override
	public int compareTo(FieldSignature other) {
		int diff = definingClass.compareAgainst(other.definingClass);
		if (diff != 0)
			return diff;

		diff = name.compareTo(other.name);
		if (diff != 0)
			return diff;
		else
			return type.compareAgainst(other.type);
	}
}