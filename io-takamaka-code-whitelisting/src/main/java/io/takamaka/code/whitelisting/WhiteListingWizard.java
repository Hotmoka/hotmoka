package io.takamaka.code.whitelisting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * An object that knows about the fields, methods and constructors that can be called from
 * Takamaka code and their proof-obligations.
 */
public interface WhiteListingWizard {

	/**
	 * Looks for a white-listing model of the given field. That is a field declaration
	 * that justifies why the field is white-listed. It can be the field itself, if it
	 * belongs to a class installed in blockchain, or otherwise a field of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param field the field whose model is looked for
	 * @return the model of its white-listing, if it exists
	 */
	Optional<Field> whiteListingModelOf(Field field);

	/**
	 * Looks for a white-listing model of the given constructor. That is a constructor declaration
	 * that justifies why the constructor is white-listed. It can be the constructor itself, if it
	 * belongs to a class installed in blockchain, or otherwise a constructor of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param constructor the constructor whose model is looked for
	 * @return the model of its white-listing, if it exists
	 * @throws ClassNotFoundException if some class could not be found during the look-up of the model
	 */
	Optional<Constructor<?>> whiteListingModelOf(Constructor<?> constructor) throws ClassNotFoundException;

	/**
	 * Looks for a white-listing model of the given method. That is a method declaration
	 * that justifies why the method is white-listed. It can be the method itself, if it
	 * belongs to a class installed in blockchain, or otherwise a method of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param method the method whose model is looked for
	 * @return the model of its white-listing, if it exists
	 * @throws ClassNotFoundException if some class could not be found during the look-up of the model
	 */
	Optional<Method> whiteListingModelOf(Method method) throws ClassNotFoundException;
}