package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a method or constructor.
 */
@Immutable
public abstract class CodeSignature implements Serializable {

	private static final long serialVersionUID = 2342747645709601285L;

	/**
	 * The class of the method or constructor.
	 */
	public final ClassType definingClass;

	/**
	 * The formal arguments of the method or constructor.
	 */
	private final StorageType[] formals;

	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	protected CodeSignature(ClassType definingClass, StorageType... formals) {
		this.definingClass = definingClass;
		this.formals = formals;
	}

	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the name of the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	public CodeSignature(String definingClass, StorageType... formals) {
		this(ClassType.mk(definingClass), formals);
	}

	/**
	 * Yields the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the formal arguments
	 */
	public final Stream<StorageType> formals() {
		return Stream.of(formals);
	}

	/**
	 * Yields a comma-separated string of the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the string
	 */
	protected final String commaSeparatedFormals() {
		return formals()
			.map(StorageType::toString)
			.collect(Collectors.joining(",", "(", ")"));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CodeSignature && ((CodeSignature) other).definingClass.equals(definingClass)
			&& Arrays.equals(((CodeSignature) other).formals, formals);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ Arrays.hashCode(formals);
	}

	/**
	 * Marshals this code signature into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the code signature cannot be marshalled
	 */
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeUTF(definingClass.name);
		oos.writeInt(formals.length);
		for (StorageType formal: formals)
			formal.into(oos);
	}

	/**
	 * Factory method that unmarshals a code signature from the given stream.
	 * 
	 * @param ois the stream
	 * @return the code signature
	 * @throws IOException if the code signature could not be unmarshalled
	 * @throws ClassNotFoundException if the code signature could not be unmarshalled
	 */
	public static CodeSignature from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		String definingClass = ois.readUTF();
		int formalsCount = ois.readInt();
		StorageType[] formals = new StorageType[formalsCount];
		for (int pos = 0; pos < formalsCount; pos++)
			formals[pos] = StorageType.from(ois);

		switch (selector) {
		case ConstructorSignature.SELECTOR: return new ConstructorSignature(definingClass, formals);
		case VoidMethodSignature.SELECTOR: return new VoidMethodSignature(definingClass, ois.readUTF(), formals);
		case NonVoidMethodSignature.SELECTOR: return new NonVoidMethodSignature(definingClass, ois.readUTF(), StorageType.from(ois), formals);
		default: throw new IOException("unexpected code signature selector: " + selector);
		}
	}
}