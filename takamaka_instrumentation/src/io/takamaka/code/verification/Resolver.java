package io.takamaka.code.verification;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * An utility that implements resolving algorithms for field and methods.
 */
public interface Resolver {

	/**
	 * Yields the field signature that would be accessed by the given instruction.
	 * 
	 * @param fi the instruction
	 * @return the signature, if any
	 */
	public Optional<Field> resolvedFieldFor(FieldInstruction fi);

	/**
	 * Yields the method or constructor signature that would be accessed by the given instruction.
	 * At run time, that signature or one of its redefinitions (for non-private non-final methods) will be called.
	 * 
	 * @param invoke the instruction
	 * @return the signature
	 */
	public Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction invoke);
}