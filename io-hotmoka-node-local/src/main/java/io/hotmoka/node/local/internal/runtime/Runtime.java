/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.local.internal.runtime;

import java.math.BigInteger;

import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.local.DeserializationException;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.builders.AbstractResponseBuilder;
import io.hotmoka.node.local.internal.builders.EngineClassLoaderImpl;
import io.hotmoka.whitelisting.Dummy;

/**
 * A class that contains utility methods called by instrumented
 * Takamaka code stored in a node. This class is not installed in
 * the node's store, hence it is not visible to Takamaka programmers
 * and needn't obey to Takamaka code constraints. It is only visible
 * at run time, since its package is opened by the module-info descriptor
 * of this module.
 */
public abstract class Runtime {

	/**
	 * A map that provides the response creation environment for each thread that is running a transaction.
	 */
	public final static ThreadLocal<AbstractResponseBuilder<?,?>.ResponseCreator> responseCreators = new ThreadLocal<>();

	/**
	 * Response builders spawn a thread when they need to execute code that could
	 * call into this class. This allows the execution of more transactions in parallel
	 * and with distinct class loaders.
	 * This method yields the transaction builder that is using that thread.
	 * 
	 * @return the transaction builder that is using the current thread
	 */
	private static AbstractResponseBuilder<?,?>.ResponseCreator getResponseCreator() {
		return responseCreators.get();
	}

	/**
	 * Yields the last value assigned to the given lazy, non-{@code final} field of the given storage object.
	 * 
	 * @param object the container of the field
	 * @param definingClass the class of the field. This can only be the class of {@code object}
	 *                      or one of its superclasses
	 * @param name the name of the field
	 * @param fieldClassName the name of the type of the field
	 * @return the value of the field
	 * @throws StoreException 
	 * @throws DeserializationException 
     */
	public static Object deserializeLastLazyUpdateFor(Object object, String definingClass, String name, String fieldClassName) throws DeserializationException, StoreException {
		AbstractResponseBuilder<?, ?>.ResponseCreator responseCreator = getResponseCreator();
		// TODO: check exception below
		return responseCreator.deserializeLastUpdateFor(responseCreator.getClassLoader().getStorageReferenceOf(object), FieldSignatures.of(StorageTypes.classNamed(definingClass), name, StorageTypes.classNamed(fieldClassName)));
	}

	/**
	 * Yields the last value assigned to the given lazy, {@code final} field of the given storage object.
	 * 
	 * @param object the container of the field
	 * @param definingClass the class of the field. This can only be the class of {@code object}
	 *                      or one of its superclasses
	 * @param name the name of the field
	 * @param fieldClassName the name of the type of the field
	 * @return the value of the field
	 * @throws StoreException 
	 * @throws DeserializationException 
     */
	public static Object deserializeLastLazyUpdateForFinal(Object object, String definingClass, String name, String fieldClassName) throws DeserializationException, StoreException {
		AbstractResponseBuilder<?,?>.ResponseCreator responseCreator = getResponseCreator();
		// TODO: check exception below
		return responseCreator.deserializeLastUpdateForFinal(responseCreator.getClassLoader().getStorageReferenceOf(object), FieldSignatures.of(StorageTypes.classNamed(definingClass), name, StorageTypes.classNamed(fieldClassName)));
	}

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor
	 * of a contract. It forwards the call to {@code io.takamaka.code.lang.Storage.entry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Storage.fromContract()}
	 */
	public static void fromContract(Object callee, Object caller) throws Throwable {
		getResponseCreator().getClassLoader().fromContract(callee, caller);
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param dummy may be used to signal something to the callee
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public static void payableFromContract(Object callee, Object caller, Dummy dummy, BigInteger amount) throws Throwable {
		EngineClassLoaderImpl classLoader = getResponseCreator().getClassLoader();
		classLoader.fromContract(callee, caller);
		if (dummy == Dummy.METHOD_ON_THIS)
			// the callee pays itself
			classLoader.payableFromContract(callee, callee, amount);
		else
			classLoader.payableFromContract(callee, caller, amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Storage.entry()} and then
	 * to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param dummy may be used to signal something to the callee
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Storage.entry()}
	 *         or {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public static void payableFromContract(Object callee, Object caller, Dummy dummy, int amount) throws Throwable {
		EngineClassLoaderImpl classLoader = getResponseCreator().getClassLoader();
		classLoader.fromContract(callee, caller);
		if (dummy == Dummy.METHOD_ON_THIS)
			// the callee pays itself
			classLoader.payableFromContract(callee, callee, amount);
		else
			classLoader.payableFromContract(callee, caller, amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Storage.entry()} and then to
	 * {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param dummy may be used to signal something to the callee
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Storage.entry()}
	 *         or {@code io.takamaka.code.lang.Contract.entry()}
	 */
	public static void payableFromContract(Object callee, Object caller, Dummy dummy, long amount) throws Throwable {
		EngineClassLoaderImpl classLoader = getResponseCreator().getClassLoader();
		classLoader.fromContract(callee, caller);
		if (dummy == Dummy.METHOD_ON_THIS)
			// the callee pays itself
			classLoader.payableFromContract(callee, callee, amount);
		else
			classLoader.payableFromContract(callee, caller, amount);
	}

	/**
	 * Takes note of the given event.
	 *
	 * @param event the event
	 */
	public static void event(Object event) {
		getResponseCreator().event(event);
	}

	/**
	 * Yields the execution time of the transaction.
	 * 
	 * @return the time
	 */
	public static long now() {
		return getResponseCreator().now();
	}

	/**
	 * Determines if the execution was started by the node itself.
	 * This is always false if the node has no notion of commit.
	 * If the execution has been started by a user request, this will
	 * always be false.
	 * 
	 * @return true if and only if that condition occurs
	 */
	public static boolean isSystemCall() {
		return getResponseCreator().isSystemCall();
	}

	/**
	 * Yields the value of field {@code inStorage} of the given storage object.
	 * 
	 * @param object the storage object
	 * @return the value of the field
	 */
	public static boolean inStorageOf(Object object) {
		return getResponseCreator().getClassLoader().getInStorageOf(object);
	}

	/**
	 * Compares the storage references of the given two storage objects, according
	 * to the natural ordering of storage references.
	 * 
	 * @param o1 the first storage object
	 * @param o2 the second storage object
	 * @return the result of the comparison
	 */
	public static int compareStorageReferencesOf(Object o1, Object o2) {
		if (o1 == o2)
			return 0;
		else if (o1 == null)
			return -1;
		else if (o2 == null)
			return 1;
		else {
			EngineClassLoader classLoader = getResponseCreator().getClassLoader();
			return classLoader.getStorageReferenceOf(o1).compareTo(classLoader.getStorageReferenceOf(o2));
		}
	}

	/**
	 * Yields the next storage reference for the current transaction.
	 * It can be used to refer to a newly created object.
	 * 
	 * @return the next storage reference
	 */
	public static Object getNextStorageReference() {
		return getResponseCreator().getNextStorageReference();
	}

	/**
	 * Charges the given amount of gas for RAM usage for the current transaction.
	 * 
	 * @param ram the amount of gas to consume for RAM consumption
	 */
	public static void chargeForRAM(BigInteger ram) {
		getResponseCreator().chargeGasForRAM(ram);
	}

	/**
	 * Charges the given amount of gas for RAM usage for the current transaction.
	 * 
	 * @param ram the amount of gas to consume for RAM consumption
	 */
	public static void chargeForRAM(long ram) {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(ram));
	}

	/**
	 * Charges the given amount of gas for RAM usage for the current transaction.
	 * 
	 * @param ram the amount of gas to consume for RAM consumption
	 */
	public static void chargeForRAM(int ram) {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(ram));
	}

	/**
	 * Charges one unit of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM1() {
		getResponseCreator().chargeGasForRAM(BigInteger.ONE);
	}

	/**
	 * Charges two units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM2() {
		getResponseCreator().chargeGasForRAM(BigInteger.TWO);
	}

	/**
	 * Charges three units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM3() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(3L));
	}

	/**
	 * Charges four units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM4() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(4L));
	}

	/**
	 * Charges five units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM5() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(5L));
	}

	/**
	 * Charges six units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM6() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(6L));
	}

	/**
	 * Charges seven units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM7() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(7L));
	}

	/**
	 * Charges eight units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM8() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(8L));
	}

	/**
	 * Charges nine units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM9() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(9L));
	}

	/**
	 * Charges ten units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM10() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(10L));
	}

	/**
	 * Charges eleven units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM11() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(11L));
	}

	/**
	 * Charges twelve units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM12() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(12L));
	}

	/**
	 * Charges thirteen units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM13() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(13L));
	}

	/**
	 * Charges fourteen units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM14() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(14L));
	}

	/**
	 * Charges fifteen units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM15() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(15L));
	}

	/**
	 * Charges sixteen units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM16() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(16L));
	}

	/**
	 * Charges seventeen units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM17() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(17L));
	}

	/**
	 * Charges eighteen units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM18() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(18L));
	}

	/**
	 * Charges nineteen units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM19() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(19L));
	}

	/**
	 * Charges twenty units of gas for RAM usage for the current transaction.
	 */
	public static void chargeForRAM20() {
		getResponseCreator().chargeGasForRAM(BigInteger.valueOf(20L));
	}

	/**
	 * Charges the given amount of gas for CPU usage for the current transaction.
	 * 
	 * @param cpu the amount of gas to consume
	 */
	public static void charge(long cpu) {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(cpu));
	}

	/**
	 * Charges the given amount of gas for CPU usage for the current transaction.
	 * 
	 * @param cpu the amount of gas to consume
	 */
	public static void charge(int cpu) {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(cpu));
	}

	/**
	 * Charges one unit of gas for CPU usage for the current transaction.
	 */
	public static void charge1() {
		getResponseCreator().chargeGasForCPU(BigInteger.ONE);
	}

	/**
	 * Charges two units of gas for CPU usage for the current transaction.
	 */
	public static void charge2() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(2L));
	}

	/**
	 * Charges three units of gas for CPU usage for the current transaction.
	 */
	public static void charge3() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(3L));
	}

	/**
	 * Charges four units of gas for CPU usage for the current transaction.
	 */
	public static void charge4() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(4L));
	}

	/**
	 * Charges five units of gas for CPU usage for the current transaction.
	 */
	public static void charge5() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(5L));
	}

	/**
	 * Charges six units of gas for CPU usage for the current transaction.
	 */
	public static void charge6() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(6L));
	}

	/**
	 * Charges seven units of gas for CPU usage for the current transaction.
	 */
	public static void charge7() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(7L));
	}

	/**
	 * Charges eight units of gas for CPU usage for the current transaction.
	 */
	public static void charge8() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(8L));
	}

	/**
	 * Charges nine units of gas for CPU usage for the current transaction.
	 */
	public static void charge9() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(9L));
	}

	/**
	 * Charges ten units of gas for CPU usage for the current transaction.
	 */
	public static void charge10() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(10L));
	}

	/**
	 * Charges eleven units of gas for CPU usage for the current transaction.
	 */
	public static void charge11() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(11L));
	}

	/**
	 * Charges twelve units of gas for CPU usage for the current transaction.
	 */
	public static void charge12() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(12L));
	}

	/**
	 * Charges 13 units of gas for CPU usage for the current transaction.
	 */
	public static void charge13() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(13L));
	}

	/**
	 * Charges 14 units of gas for CPU usage for the current transaction.
	 */
	public static void charge14() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(14L));
	}

	/**
	 * Charges 15 units of gas for CPU usage for the current transaction.
	 */
	public static void charge15() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(15L));
	}

	/**
	 * Charges 16 units of gas for CPU usage for the current transaction.
	 */
	public static void charge16() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(16L));
	}

	/**
	 * Charges 17 units of gas for CPU usage for the current transaction.
	 */
	public static void charge17() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(17L));
	}

	/**
	 * Charges 18 units of gas for CPU usage for the current transaction.
	 */
	public static void charge18() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(18L));
	}

	/**
	 * Charges 19 units of gas for CPU usage for the current transaction.
	 */
	public static void charge19() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(19L));
	}

	/**
	 * Charges 20 units of gas for CPU usage for the current transaction.
	 */
	public static void charge20() {
		getResponseCreator().chargeGasForCPU(BigInteger.valueOf(20L));
	}
}