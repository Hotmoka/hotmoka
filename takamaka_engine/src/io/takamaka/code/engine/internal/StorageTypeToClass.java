package io.takamaka.code.engine.internal;

import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;

/**
 * An object that translates storage types into their run-time class tag.
 */
public class StorageTypeToClass {

	/**
	 * The blockchain for which the translation is performed.
	 */
	private final AbstractTransactionRun<?,?> run;

	/**
	 * Builds an object that translates storage types into their run-time class tag.
	 * 
	 * @param run the blockchain for which the translation is performed
	 */
	public StorageTypeToClass(AbstractTransactionRun<?,?> run) {
		this.run = run;
	}

	/**
	 * Yields the class object that represents the given storage type in the Java language,
	 * for the current transaction.
	 * 
	 * @param type the storage type
	 * @return the class object, if any
	 * @throws ClassNotFoundException if some class type cannot be found
	 */
	public Class<?> toClass(StorageType type) throws ClassNotFoundException {
		if (type instanceof BasicTypes) {
			switch ((BasicTypes) type) {
			case BOOLEAN: return boolean.class;
			case BYTE: return byte.class;
			case CHAR: return char.class;
			case SHORT: return short.class;
			case INT: return int.class;
			case LONG: return long.class;
			case FLOAT: return float.class;
			case DOUBLE: return double.class;
			}
		}
		else if (type instanceof ClassType)
			return run.classLoader.loadClass(((ClassType) type).name);
	
		throw new IllegalArgumentException("unexpected storage type");
	}
}