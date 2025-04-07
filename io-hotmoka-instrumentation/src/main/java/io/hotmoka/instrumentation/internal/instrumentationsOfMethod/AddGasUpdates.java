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

package io.hotmoka.instrumentation.internal.instrumentationsOfMethod;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.AllocationInstruction;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ExceptionThrower;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Select;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.whitelisting.WhitelistingConstants;

/**
 * Adds a gas decrease at the beginning of each basic block of code or
 * before instructions that allocate memory.
 */
public class AddGasUpdates extends MethodLevelInstrumentation {
	private final static ObjectType RUNTIME_OT = new ObjectType(WhitelistingConstants.RUNTIME_NAME);
	private final static ObjectType BIGINTEGER_OT = new ObjectType(BigInteger.class.getName());
	private final static Type[] ONE_BIGINTEGER_ARGS = { BIGINTEGER_OT };
	private final static Type[] ONE_INT_ARGS = { Type.INT };
	private final static Type[] ONE_LONG_ARGS = { Type.LONG };
	private final static short PRIVATE_SYNTHETIC_STATIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_STATIC;

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 * @param method the method being instrumented
	 * @throws IllegalJarException if the jar under instrumentation is not legal
	 * @throws UnknownTypeException if some type of the jar under instrumentation cannot be resolved
	 */
	public AddGasUpdates(InstrumentedClassImpl.Builder builder, MethodGen method) throws IllegalJarException, UnknownTypeException {
		builder.super(method);

		if (!method.isAbstract()) {
			SortedSet<InstructionHandle> dominators = computeDominators(method);
			InstructionList il = method.getInstructionList();
			CodeExceptionGen[] ceg = method.getExceptionHandlers();

			dominators.forEach(dominator -> addCpuGasUpdate(dominator, il, ceg, dominators));
			for (var ih: il)
				addRamGasUpdate(ih, il, ceg);
		}
	}

	/**
	 * Computes the set of dominators of the given method or constructor, that is,
	 * the instructions where basic blocks start.
	 * 
	 * @param method the method whose dominators must be computed
	 * @return the set of dominators, ordered in increasing position
	 */
	private SortedSet<InstructionHandle> computeDominators(MethodGen method) {
		return StreamSupport.stream(method.getInstructionList().spliterator(), false)
			.filter(this::isDominator)
			.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(InstructionHandle::getPosition))));
	}

	private boolean isDominator(InstructionHandle ih) {
		InstructionHandle prev = ih.getPrev();
		// the first instruction is a dominator
		return prev == null || prev.getInstruction() instanceof BranchInstruction
			|| prev.getInstruction() instanceof ExceptionThrower
			|| Stream.of(ih.getTargeters()).anyMatch(targeter -> targeter instanceof BranchInstruction || targeter instanceof CodeExceptionGen);
	}

	private void addRamGasUpdate(InstructionHandle ih, InstructionList il, CodeExceptionGen[] ceg) throws IllegalJarException, UnknownTypeException {
		Instruction bytecode = ih.getInstruction();

		if (bytecode instanceof InvokeInstruction invoke) {
			ReferenceType receiver = invoke.getReferenceType(cpg);
			// we do not count the calls due to instrumentation, such as those for gas metering themselves
			if (!RUNTIME_OT.equals(receiver)) {
				// we compute an estimation of the size of the activation frame for the callee
				long size = invoke.getArgumentTypes(cpg).length;
				if (invoke instanceof INVOKEVIRTUAL || invoke instanceof INVOKESPECIAL || invoke instanceof INVOKEINTERFACE)
					size++;

				// no risk of overflow, since there are at most 256 arguments in a method
				size *= gasCostModel.ramCostOfActivationSlot();
				size += gasCostModel.ramCostOfActivationRecord();

				addRamGasUpdate(size, ih, il, ceg);
			}
		}
		else if (bytecode instanceof NEW _new) {
			ObjectType createdClass = _new.getLoadClassType(cpg);
			long size = gasCostModel.ramCostOfObject() + numberOfInstanceFieldsOf(createdClass) * (long) gasCostModel.ramCostOfField();
			addRamGasUpdate(size, ih, il, ceg);
		}
		else if (bytecode instanceof NEWARRAY || bytecode instanceof ANEWARRAY) {
			// the behavior of getType() is different between the two instructions;
			// it yields the created array type for NEWARRAY, while it gets the type
			// of the elements of the created array type for ANEWARRAY
			Type createdType = bytecode instanceof NEWARRAY na ?
				na.getType() :
				new ArrayType(((ANEWARRAY) bytecode).getType(cpg), 1); // cast holds by exclusion
			String allocatorName = getNewNameForPrivateMethod(InstrumentationConstants.EXTRA_ALLOCATOR);
			String bigInteger = BigInteger.class.getName();
			InvokeInstruction valueOf = factory.createInvoke(bigInteger, "valueOf", BIGINTEGER_OT, ONE_LONG_ARGS, Const.INVOKESTATIC);
			InvokeInstruction multiply = factory.createInvoke(bigInteger, "multiply", BIGINTEGER_OT, ONE_BIGINTEGER_ARGS, Const.INVOKEVIRTUAL);
			InvokeInstruction add = factory.createInvoke(bigInteger, "add", BIGINTEGER_OT, ONE_BIGINTEGER_ARGS, Const.INVOKEVIRTUAL);

			var allocatorIl = new InstructionList();
			allocatorIl.append(InstructionConst.ILOAD_0);
			allocatorIl.append(InstructionConst.I2L);
			allocatorIl.append(valueOf);
			allocatorIl.append(factory.createConstant((long) gasCostModel.ramCostOfArraySlot()));
			allocatorIl.append(valueOf);
			allocatorIl.append(multiply);
			allocatorIl.append(factory.createConstant((long) gasCostModel.ramCostOfArray()));
			allocatorIl.append(valueOf);
			allocatorIl.append(add);
			// we charge the gas
			allocatorIl.append(factory.createInvoke(WhitelistingConstants.RUNTIME_NAME, "chargeForRAM", Type.VOID, ONE_BIGINTEGER_ARGS, Const.INVOKESTATIC));
			// this is where to jump to create the array
			InstructionHandle creation = allocatorIl.append(InstructionConst.ILOAD_0);
			// the allocation is moved into the allocator method
			allocatorIl.append(bytecode);
			allocatorIl.append(InstructionConst.ARETURN);

			// if the size of the array is negative, no gas is charged and the array creation bytecode will throw an exception
			allocatorIl.insert(InstructionFactory.createBranchInstruction(Const.IFLT, creation));
			allocatorIl.insert(InstructionConst.ILOAD_0);

			addMethod(new MethodGen(PRIVATE_SYNTHETIC_STATIC, createdType, ONE_INT_ARGS, null, allocatorName, className, allocatorIl, cpg));

			// the original array creation bytecode gets replaced with a call to the allocation method
			ih.setInstruction(factory.createInvoke(className, allocatorName, createdType, ONE_INT_ARGS, Const.INVOKESTATIC));
		}
		else if (bytecode instanceof MULTIANEWARRAY multianewarray) {
			Type createdType = multianewarray.getType(cpg);
			// this bytecode might only create some dimensions of the created array type 
			int createdDimensions = multianewarray.getDimensions();
			if (createdDimensions <= 0)
				throw new IllegalJarException("multianewarray with non-positive created dimensions");

			Type[] args = IntStream.range(0, createdDimensions)
				.mapToObj(dim -> Type.INT)
				.toArray(Type[]::new);
			String allocatorName = getNewNameForPrivateMethod(InstrumentationConstants.EXTRA_ALLOCATOR);
			InstructionList allocatorIl = new InstructionList();
			IntStream.range(0, createdDimensions)
				.mapToObj(dim -> InstructionFactory.createLoad(Type.INT, dim))
				.forEach(allocatorIl::append);

			// the allocation is moved into the allocator method
			allocatorIl.append(multianewarray);
			allocatorIl.append(InstructionConst.ARETURN);

			// this is where to jump to create the array
			InstructionHandle creation = allocatorIl.getStart();

			// where to jump if the last dimension is negative: of course this will lead to a run-time exception
			// since dimensions of multianewarray must be non-negative
			InstructionHandle fallBack = allocatorIl.insert(InstructionConst.POP2);

			// where to jump if a dimension is negative: of course this will lead to a run-time exception
			// since dimensions of multianewarray must be non-negative
			allocatorIl.insert(InstructionFactory.createBranchInstruction(Const.GOTO, creation));
			allocatorIl.insert(InstructionConst.POP2);
			InstructionHandle fallBack2 = allocatorIl.insert(InstructionConst.POP);

			String bigInteger = BigInteger.class.getName();
			InvokeInstruction valueOf = factory.createInvoke(bigInteger, "valueOf", BIGINTEGER_OT, ONE_LONG_ARGS, Const.INVOKESTATIC);
			InvokeInstruction multiply = factory.createInvoke(bigInteger, "multiply", BIGINTEGER_OT, ONE_BIGINTEGER_ARGS, Const.INVOKEVIRTUAL);
			InvokeInstruction add = factory.createInvoke(bigInteger, "add", BIGINTEGER_OT, ONE_BIGINTEGER_ARGS, Const.INVOKEVIRTUAL);

			// we start from 1
			allocatorIl.insert(fallBack2, factory.createGetStatic(bigInteger, "ONE", BIGINTEGER_OT));

			// we multiply all dimensions but one, computing over BigInteger, to infer the number of arrays that get created
			IntStream.range(0, createdDimensions - 1).forEach(dimension -> {
				allocatorIl.insert(fallBack2, InstructionFactory.createLoad(Type.INT, dimension));
				allocatorIl.insert(fallBack2, InstructionConst.DUP);
				allocatorIl.insert(fallBack2, InstructionFactory.createBranchInstruction(Const.IFLT, fallBack));
				allocatorIl.insert(fallBack2, InstructionConst.I2L);
				allocatorIl.insert(fallBack2, valueOf);
				allocatorIl.insert(fallBack2, multiply);
			});

			// the number of arrays is duplicated and left below the stack, adding a unit for the main array
			// and multiplying for the cost of a single array
			allocatorIl.insert(fallBack2, InstructionConst.DUP);
			allocatorIl.insert(fallBack2, factory.createGetStatic(bigInteger, "ONE", BIGINTEGER_OT));
			allocatorIl.insert(fallBack2, add);
			allocatorIl.insert(fallBack2, factory.createConstant((long) gasCostModel.ramCostOfArray()));
			allocatorIl.insert(fallBack2, valueOf);	
			allocatorIl.insert(fallBack2, multiply);
			allocatorIl.insert(fallBack2, InstructionConst.SWAP);

			// the last dimension is computed apart, since it contributes to the elements only,
			// but not to the number of created arrays
			allocatorIl.insert(fallBack2, InstructionFactory.createLoad(Type.INT, createdDimensions - 1));
			allocatorIl.insert(fallBack2, InstructionConst.DUP);
			allocatorIl.insert(fallBack2, InstructionFactory.createBranchInstruction(Const.IFLT, fallBack2));
			allocatorIl.insert(fallBack2, InstructionConst.I2L);
			allocatorIl.insert(fallBack2, valueOf);
			allocatorIl.insert(fallBack2, multiply);

			// we multiply the number of elements for the RAM cost of a single element
			allocatorIl.insert(fallBack2, factory.createConstant((long) gasCostModel.ramCostOfArraySlot()));
			allocatorIl.insert(fallBack2, valueOf);	
			allocatorIl.insert(fallBack2, multiply);

			// we add the cost of the arrays
			allocatorIl.insert(fallBack2, add);

			// we charge the gas
			allocatorIl.insert(fallBack2, factory.createInvoke(WhitelistingConstants.RUNTIME_NAME, "chargeForRAM", Type.VOID, ONE_BIGINTEGER_ARGS, Const.INVOKESTATIC));
			allocatorIl.insert(fallBack2, InstructionFactory.createBranchInstruction(Const.GOTO, creation));

			addMethod(new MethodGen(PRIVATE_SYNTHETIC_STATIC, createdType, args, null, allocatorName, className, allocatorIl, cpg));

			// the original multianewarray gets replaced with a call to the allocation method
			ih.setInstruction(factory.createInvoke(className, allocatorName, createdType, args, Const.INVOKESTATIC));
		}
	}

	private int numberOfInstanceFieldsOf(ObjectType type) throws UnknownTypeException {
		int size = 0;

		try {
			for (Class<?> clazz = classLoader.loadClass(type.getClassName()); clazz != Object.class; clazz = clazz.getSuperclass())
				size += Stream.of(clazz.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).count();
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(type.getClassName());
		}

		return size;
	}

	private void addCpuGasUpdate(InstructionHandle dominator, InstructionList il, CodeExceptionGen[] ceg, SortedSet<InstructionHandle> dominators) {
		long cost = cpuCostOf(dominator, dominators);
		InstructionHandle newTarget;

		// we check if there is an optimized charge method for the cost
		if (cost <= InstrumentationConstants.MAX_OPTIMIZED_CHARGE)
			newTarget = il.insert(dominator, factory.createInvoke(WhitelistingConstants.RUNTIME_NAME, "charge" + cost, Type.VOID, Type.NO_ARGS, Const.INVOKESTATIC));
		else {
			newTarget = il.insert(dominator, createConstantPusher(cost));
			il.insert(dominator, chargeCall(cost, "charge"));
		}

		il.redirectBranches(dominator, newTarget);
		il.redirectExceptionHandlers(ceg, dominator, newTarget);
	}

	private void addRamGasUpdate(long cost, InstructionHandle ih, InstructionList il, CodeExceptionGen[] ceg) {
		InstructionHandle newTarget;

		// we check if there is an optimized charge method for the cost
		if (cost <= InstrumentationConstants.MAX_OPTIMIZED_CHARGE)
			newTarget = il.insert(ih, factory.createInvoke(WhitelistingConstants.RUNTIME_NAME, "chargeForRAM" + cost, Type.VOID, Type.NO_ARGS, Const.INVOKESTATIC));
		else {
			newTarget = il.insert(ih, createConstantPusher(cost));
			il.insert(ih, chargeCall(cost, "chargeForRAM"));
		}

		il.redirectBranches(ih, newTarget);
		il.redirectExceptionHandlers(ceg, ih, newTarget);
	}

	private InvokeInstruction chargeCall(long value, String name) {
		return factory.createInvoke(WhitelistingConstants.RUNTIME_NAME, name, Type.VOID, value < Integer.MAX_VALUE ? ONE_INT_ARGS : ONE_LONG_ARGS, Const.INVOKESTATIC);
	}

	private Instruction createConstantPusher(long value) {
		// we determine if we can use an integer or we need a long (highly unlikely...)
		if (value < Integer.MAX_VALUE)
			return factory.createConstant((int) value);
		else
			return factory.createConstant(value);
	}

	private long cpuCostOf(InstructionHandle dominator, SortedSet<InstructionHandle> dominators) {
		long cost = 0L;

		InstructionHandle cursor = dominator;
		do {
			Instruction instruction = cursor.getInstruction();
			if (instruction instanceof ArithmeticInstruction)
				cost += gasCostModel.cpuCostOfArithmeticInstruction();
			else if (instruction instanceof ArrayInstruction)
				cost += gasCostModel.cpuCostOfArrayAccessInstruction();
			else if (instruction instanceof FieldInstruction)
				cost += gasCostModel.cpuCostOfFieldAccessInstruction();
			else if (instruction instanceof InvokeInstruction)
				cost += gasCostModel.cpuCostOfInvokeInstruction();
			else if (instruction instanceof AllocationInstruction)
				cost += gasCostModel.cpuCostOfMemoryAllocationInstruction();
			else if (instruction instanceof Select)
				cost += gasCostModel.cpuCostOfSelectInstruction();
			else
				cost += gasCostModel.cpuCostOfInstruction();

			cursor = cursor.getNext();
		}
		while (cursor != null && !dominators.contains(cursor));

		return cost;
	}
}