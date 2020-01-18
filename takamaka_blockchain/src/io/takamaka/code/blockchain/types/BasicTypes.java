package io.takamaka.code.blockchain.types;

import io.takamaka.code.blockchain.annotations.Immutable;

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
	public int compareAgainst(StorageType other) {
		return other instanceof BasicTypes ? compareTo((BasicTypes) other)
			: -1; // other instanceof ClassType
	}
}