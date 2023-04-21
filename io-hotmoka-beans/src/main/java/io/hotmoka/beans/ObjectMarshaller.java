package io.hotmoka.beans;

import java.io.IOException;

/**
 * Knowledge about how an object of a given class can be marshalled.
 * This can be used to provide the ability to marshall objects of arbitrary classes.
 * 
 * @param <C> the type of the class
 */
abstract class ObjectMarshaller<C> {
	final Class<C> clazz;
	
	protected ObjectMarshaller(Class<C> clazz) {
		this.clazz = clazz;
	}

	/**
	 * How an object of class <code>C</code> can be marshalled.
	 * 
	 * @param value the value to marshall
	 * @param context the marshalling context
	 * @return the unmarshalled object
	 * @throws IOException if the object could not be marshalled
	 */
	public abstract void write(C value, MarshallingContext context) throws IOException;
}