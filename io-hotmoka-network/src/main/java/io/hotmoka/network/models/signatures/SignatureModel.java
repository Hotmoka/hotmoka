package io.hotmoka.network.models.signatures;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The model of the signature of a field, method or constructor.
 */
public abstract class SignatureModel {

	/**
	 * The name of the class defining the field, method or constructor.
	 */
	public String definingClass;

	/**
	 * Builds the model of the signature of a field, method or constructor.
	 * 
	 * @param definingClass the name of the class defining the field, method or constructor
	 */
	protected SignatureModel(String definingClass) {
		this.definingClass = definingClass;
	}

	protected SignatureModel() {}

	/**
	 * Yields a string representation of the given type.
	 * 
	 * @param type the type
	 * @return the string
	 */
	protected static String nameOf(StorageType type) {
    	if (type == null)
    		throw new InternalFailureException("unexpected null type");
    	else if (type instanceof BasicTypes)
    		return type.toString();
    	else if (type instanceof ClassType)
    		return ((ClassType) type).name;
    	else
    		throw new InternalFailureException("unexpected storage type of class " + type.getClass().getName());
    }

	/**
	 * Yields the type with the given name.
	 * 
	 * @param name the name of the type
	 * @return the type
	 */
	protected static StorageType typeWithName(String name) {
    	if (name == null)
    		throw new InternalFailureException("unexpected null type name");

    	switch (name) {
    	case "boolean":
            return BasicTypes.BOOLEAN;
        case "byte":
            return BasicTypes.BYTE;
        case "char":
            return BasicTypes.CHAR;
        case "short":
            return BasicTypes.SHORT;
        case "int":
            return BasicTypes.INT;
        case "long":
            return BasicTypes.LONG;
        case "float":
            return BasicTypes.FLOAT;
        case "double":
            return BasicTypes.DOUBLE;
        default:
        	return new ClassType(name);
    	}
	}
}