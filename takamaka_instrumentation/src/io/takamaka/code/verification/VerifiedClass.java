package io.takamaka.code.verification;

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

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.Issue;

/**
 * A class that passed the static Takamaka verification tests.
 */
public interface VerifiedClass extends Comparable<VerifiedClass> {

	/**
	 * Builds and verifies a class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param jar the jar this class belongs to
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is built during blockchain initialization
	 * @return the new verified class
	 * @throws VefificationException if the class could not be verified
	 */
	static VerifiedClass of(JavaClass clazz, VerifiedJar jar, Consumer<Issue> issueHandler, boolean duringInitialization) {
		return new VerifiedClassImpl(clazz, jar, issueHandler, duringInitialization);
	}

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
	 * Yields the resolver object for the fields and methods of this class.
	 * 
	 * @return the resolver object
	 */
	Resolver getResolver();

	/**
	 * Yields the fully-qualified name of this class.
	 * 
	 * @return the fully-qualified name
	 */
	String getClassName();

	/**
	 * Yields the constant pool of this class.
	 * 
	 * @return the constant pool
	 */
	ConstantPoolGen getConstantPool();

	/**
	 * Yields the name of the superclass of this class, if any.
	 * 
	 * @return the name
	 */
	String getSuperclassName();

	/**
	 * Sets the name of the superclass of this class.
	 * 
	 * @param name the new name of the superclass of this clas
	 */
	void setSuperclassName(String name);

	/**
	 * Yields the attributes of this class.
	 * 
	 * @return the attributes of this class
	 */
	Attribute[] getAttributes();

	/**
	 * Adds the given field to this class.
	 * 
	 * @param field the field to add
	 */
	void addField(org.apache.bcel.classfile.Field field);

	/**
	 * Adds the given method to this class.
	 * 
	 * @param method the method to add
	 */
	void addMethod(Method method);

	/**
	 * Yields the methods in this class.
	 * 
	 * @return the methods
	 */
	Method[] getMethods();

	/**
	 * Yields the fields in this class.
	 * 
	 * @return the fields
	 */
	org.apache.bcel.classfile.Field[] getFields();

	/**
	 * Replaces a method of this class with another.
	 * 
	 * @param old the old method to replace
	 * @param _new the new method to put at its place
	 */
	void replaceMethod(Method old, Method _new);

	/**
	 * Replaces a field of this class with another.
	 * 
	 * @param old the old field to replace
	 * @param _new the new field to put at its place
	 */
	void replaceField(org.apache.bcel.classfile.Field old, org.apache.bcel.classfile.Field _new);

	/**
	 * Yields a Java class generated from this object.
	 * 
	 * @return the Java class
	 */
	JavaClass getJavaClass();
}