package io.hotmoka.beans;

import java.io.IOException;

/**
 * Knowledge about how an object of a given class can be unmarshalled.
 * This can be used to provide the ability to unmarshall objects of arbitrary classes.
 * 
 * @param <C> the type of the class
 */
abstract class ObjectUnmarshaller<C> {
	final Class<C> clazz;
	
	protected ObjectUnmarshaller(Class<C> clazz) {
		this.clazz = clazz;
	}

	/**
	 * How an object of class <code>C</code> can be unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the unmarshalled object
	 * @throws IOException if the object could not be unmarshalled
	 */
	public abstract C read(UnmarshallingContext context) throws IOException;
}