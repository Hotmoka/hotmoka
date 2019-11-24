package io.takamaka.code.instrumentation;

import org.apache.bcel.classfile.JavaClass;

import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;
import io.takamaka.code.verification.VerifiedClass;

/**
 * An instrumenter of a single class file. For instance, it instruments storage
 * classes, by adding the serialization support, and contracts, to deal with entries.
 */
public interface InstrumentedClass {

	/**
	 * Yields an instrumented class from a verified class.
	 * 
	 * @param clazz the class to instrument
	 * @param gasCostModel the gas cost model used for the instrumentation
	 */
	static InstrumentedClass of(VerifiedClass clazz, GasCostModel gasCostModel) {
		return new InstrumentedClassImpl(clazz, gasCostModel);
	}

	/**
	 * Yields the fully-qualified name of this class.
	 * 
	 * @return the fully-qualified name
	 */
	String getClassName();

	/**
	 * Yields a Java class from this object.
	 * 
	 * @return the Java class
	 */
	JavaClass toJavaClass();
}