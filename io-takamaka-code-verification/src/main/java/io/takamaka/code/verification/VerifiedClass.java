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
	 * This prefix is forbidden in the name of fields and methods of a Takamaka class,
	 * since it will be used for instrumentation. Java compilers do not allow one to
	 * use this character in the name of fields or methods, but it is still possible if
	 * Java bytecode is produced in other ways. Hence it is necessary to check that it is not used.
	 */
	String FORBIDDEN_PREFIX = "§";

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
	 * Yields a deep copy of the utility object that knows about the bootstraps of this class.
	 * It yields a deep copy since it is a modifiable object.
	 * 
	 * @return the utility object
	 */
	Bootstraps getBootstraps();

	/**
	 * Yields the utility that allows one to compute the pushers of values on the stack
	 * for the code in this class.
	 * 
	 * @return the utility object
	 */
	Pushers getPushers();

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