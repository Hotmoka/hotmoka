package io.takamaka.code.verification;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * A class that passed the static Takamaka verification tests.
 * They are ordered wrt their name.
 */
public interface VerifiedClass extends Comparable<VerifiedClass> {

	/**
	 * Yields the white-listing model for the field accessed by the given instruction.
	 * This means that that instruction accesses that field but that access is white-listed
	 * only if the resulting model is verified.
	 * 
	 * @param fi the instruction that accesses the field
	 * @return the model. This must exist, since the class is verified and all accesses have been proved
	 *         to be white-listed (up to possible proof obligations contained in the model).
	 */
	Field whiteListingModelOf(FieldInstruction fi);

	/**
	 * Yields the white-listing model for the method called by the given instruction.
	 * This means that that instruction calls that method but that call is white-listed
	 * only if the resulting model is verified.
	 * 
	 * @param invoke the instruction that calls the method
	 * @return the model. This must exist, since the class is verified and all calls have been proved
	 *         to be white-listed (up to possible proof obligations contained in the model).
	 */
	Executable whiteListingModelOf(InvokeInstruction invoke);

	/**
	 * Yields the jar this class belongs to.
	 * 
	 * @return the jar
	 */
	VerifiedJar getJar();

	/**
	 * Yields the utility object that knows about the bootstraps of this class.
	 * 
	 * @return the utility object
	 */
	Bootstraps getBootstraps();

	/**
	 * Yields the fully-qualified name of this class.
	 * 
	 * @return the fully-qualified name
	 */
	String getClassName();

	/**
	 * Builds a Java class from this object.
	 * 
	 * @return the Java class
	 */
	JavaClass toJavaClass();
}