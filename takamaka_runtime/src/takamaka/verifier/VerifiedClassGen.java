package takamaka.verifier;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import takamaka.translator.TakamakaClassLoader;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A class that passed the static Takamaka verification tests.
 */
public interface VerifiedClassGen extends Comparable<VerifiedClassGen> {

	/**
	 * Yields a verified class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param classLoader the Takamaka class loader for the context of the class
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is built during blockchain initialization
	 * @return the verified class
	 * @throws VefificationException if the class could not be verified
	 */

	static VerifiedClassGen of(JavaClass clazz, TakamakaClassLoader classLoader, Consumer<Issue> issueHandler, boolean duringInitialization) {
		return new VerifiedClassGenImpl(clazz, classLoader, issueHandler, duringInitialization);
	}

	/**
	 * Yields the class loader used to load this class and the other classes of the program it belongs to.
	 * 
	 * @return the class loader
	 */
	TakamakaClassLoader getClassLoader();

	/**
	 * Yields the name of this class.
	 * 
	 * @return the fully-qualified JVM name
	 */
	String getClassName();

	/**
	 * Yields the name of the superclass of this class.
	 * 
	 * @return the JVM, fully-qualified name
	 */
	String getSuperclassName();

	/**
	 * Yields an object that provides utility methods about lambda bootstraps in this class.
	 * 
	 * @return the utility
	 */
	ClassBootstraps getClassBootstraps();

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
	 * Yields the methods inside this class, in generator form.
	 * 
	 * @return the methods inside this class
	 */
	Stream<MethodGen> getMethodGens();

	/**
	 * Yields the constant pool of this class.
	 * 
	 * @return the constant pool
	 */
	ConstantPoolGen getConstantPool();

	/**
	 * Yields the attributes of this class.
	 * 
	 * @return the attributes
	 */
	Attribute[] getAttributes();

	/**
	 * Yields the Java class corresponding to this class generator.
	 * 
	 * @return the Java class
	 */
	JavaClass getJavaClass();

	/**
	 * The methods in this class.
	 * 
	 * @return the methods
	 */
	Method[] getMethods();

	/**
	 * Replaces a method with another in this class.
	 * 
	 * @param old the method to replace
	 * @param _new the method to use instead
	 */
	void replaceMethod(Method old, Method _new);

	/**
	 * Adds the given method to this class.
	 * 
	 * @param method the method to add
	 */
	void addMethod(Method method);

	/**
	 * Yields the fields in this class.
	 * 
	 * @return the fields
	 */
	org.apache.bcel.classfile.Field[] getFields();

	/**
	 * Replaces a field in this class with another.
	 * 
	 * @param old the field to replace
	 * @param _new the field to use instead
	 */
	void replaceField(org.apache.bcel.classfile.Field old, org.apache.bcel.classfile.Field _new);

	/**
	 * Adds the given field to this class.
	 * 
	 * @param field the field to add
	 */
	void addField(org.apache.bcel.classfile.Field field);
}