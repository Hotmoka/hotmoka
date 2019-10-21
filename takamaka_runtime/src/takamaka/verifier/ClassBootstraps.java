package takamaka.verifier;

import java.lang.reflect.Executable;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.MethodGen;

/**
 * An object that provides utility methods about the lambda bootstraps
 * contained in a class.
 */
public interface ClassBootstraps {

	/**
	 * Determines if the given bootstrap method is a method reference to an entry.
	 * 
	 * @param bootstrap the bootstrap method
	 * @return true if and only if that condition holds
	 */
	boolean lambdaIsEntry(BootstrapMethod bootstrap);

	/**
	 * Yields the bootstrap methods in this class.
	 * 
	 * @return the bootstrap methods
	 */
	Stream<BootstrapMethod> getBootstraps();

	/**
	 * Yields the subset of the bootstrap methods of this class that lead to an entry,
	 * possibly indirectly.
	 * 
	 * @return the bootstrap methods that lead to an entry
	 */
	Stream<BootstrapMethod> getBootstrapsLeadingToEntries();

	/**
	 * Yields the bootstrap method associated with the given instruction.
	 * 
	 * @param invokedynamic the instruction
	 * @return the bootstrap method
	 */
	BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic);

	/**
	 * Yields the target method or constructor called by the given bootstrap. It can also be outside
	 * the class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @return the target called method or constructor
	 */
	Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap);

	/**
	 * Yields the lambda bridge method called by the given bootstrap.
	 * It must belong to the same class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @return the lambda bridge method
	 */
	Optional<MethodGen> getLambdaFor(BootstrapMethod bootstrap);
}