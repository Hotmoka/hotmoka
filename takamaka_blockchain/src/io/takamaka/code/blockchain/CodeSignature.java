package io.takamaka.code.blockchain;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.types.StorageType;

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
	 * The size of this code signature, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot()).add(definingClass.size(gasCostModel))
			.add(formals().map(type -> type.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add));
	}
}