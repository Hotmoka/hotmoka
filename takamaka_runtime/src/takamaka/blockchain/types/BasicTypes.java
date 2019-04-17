package takamaka.blockchain.types;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * The basic types of the Takamaka language.
 */
@Immutable
public enum BasicTypes implements StorageType {
	BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE;

	@Override
	public String toString() {
		switch (this) {
		case BOOLEAN: return "boolean";
		case BYTE: return "byte";
		case CHAR: return "char";
		case SHORT: return "short";
		case INT: return "int";
		case LONG: return "long";
		case FLOAT: return "float";
		default: return "double";
		}
	}

	@Override
	public Class<?> toClass(AbstractBlockchain classLoader) {
		switch (this) {
		case BOOLEAN: return boolean.class;
		case BYTE: return byte.class;
		case CHAR: return char.class;
		case SHORT: return short.class;
		case INT: return int.class;
		case LONG: return long.class;
		case FLOAT: return float.class;
		default: return double.class;
		}
	}

	@Override
	public int compareAgainst(StorageType other) {
		if (other instanceof BasicTypes)
			return compareTo((BasicTypes) other);
		else
			return -1; // other instanceof ClassType
	}

	@Override
	public boolean isLazy() {
		return false;
	}
}