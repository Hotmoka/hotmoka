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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.AddExtraArgsToCallsToFromContract.LoadCaller;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.whitelisting.WhitelistingConstants;

/**
 * Sets the caller at the beginning of {@code @@FromContract} and updates the balance
 * at the beginning of payable {@code @@FromContract}.
 */
public class SetCallerAndBalanceAtTheBeginningOfFromContracts extends MethodLevelInstrumentation {
	private final static ObjectType CONTRACT_OT = new ObjectType(io.takamaka.code.constants.Constants.CONTRACT_NAME);
	private final static ObjectType OBJECT_OT = new ObjectType(Object.class.getName());
	private final static ObjectType DUMMY_OT = new ObjectType(WhitelistingConstants.DUMMY_NAME);
	private final static Type[] FROM_CONTRACT_ARGS = { OBJECT_OT, OBJECT_OT };

	/**
	 * The maximal number of bytecodes that can be considered during the lookup.
	 * This is important to bound the execution time of this lookup algorithm.
	 * For normal programs, it's enough to consider only a few bytecodes to find the pushers
	 * of a stack element, hence this limit is not so relevant.
	 */
	private final static int MAX_BYTECODES = 1000;

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 * @param method the method being instrumented
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 */
	public SetCallerAndBalanceAtTheBeginningOfFromContracts(InstrumentedClassImpl.Builder builder, MethodGen method) throws IllegalJarException {
		builder.super(method);

		if (isStorage || isInterface) {
			String name = method.getName();
			Type[] args = method.getArgumentTypes();
			Type returnType = method.getReturnType();

			try {
				Optional<Class<?>> ann = annotations.getFromContractArgument(className, name, args, returnType);
				if (ann.isPresent()) {
					boolean isPayable = annotations.isPayable(className, name, args, returnType);
					instrumentFromContract(method, ann.get(), isPayable);
				};
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}
	}

	/**
	 * Instruments a {@code @@FromContract} method or constructor, by setting the caller and transferring funds for payable {@code @@FromContract}s.
	 * 
	 * @param method the {@code @@FromContract} method or constructor
	 * @param callerContract the class of the caller contract
	 * @param isPayable true if and only if the {@code @@FromContract} method or constructor is payable
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 */
	private void instrumentFromContract(MethodGen method, Class<?> callerContract, boolean isPayable) throws IllegalJarException {
		// slotForCaller is the local variable used for the extra "caller" parameter;
		int slotForCaller = addExtraParameters(method);
		if (!method.isAbstract()) {
			// we shift one slot upwards all local variables from slotForCaller, inclusive
			shiftUp(method, slotForCaller);
			setCallerAndBalance(method, callerContract, slotForCaller, isPayable);
		}
	}

	private void shiftUp(MethodGen method, int slotForCaller) {
		for (InstructionHandle ih: method.getInstructionList()) {
			Instruction ins = ih.getInstruction();
			if (ins instanceof LocalVariableInstruction lvi && !(lvi instanceof LoadCaller)) {
				int local = lvi.getIndex();
				if (local >= slotForCaller) {
					if (ins instanceof IINC iinc)
						ih.setInstruction(new IINC(local + 1, iinc.getIncrement()));
					else if (ins instanceof LoadInstruction li)
						ih.setInstruction(InstructionFactory.createLoad(li.getType(cpg), local + 1));
					else if (ins instanceof StoreInstruction si)
						ih.setInstruction(InstructionFactory.createStore(si.getType(cpg), local + 1));
				}
			}
		}
	}

	/**
	 * Instruments a {@code @@FromContract} method or constructor by calling the runtime method that sets caller and balance.
	 * 
	 * @param method the {@code @@FromContract} method or constructor
	 * @param callerContract the class of the caller contract
	 * @param slotForCaller the local variable for the caller implicit argument
	 * @param isPayable true if and only if {@code method} is payable
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 */
	private void setCallerAndBalance(MethodGen method, Class<?> callerContract, int slotForCaller, boolean isPayable) throws IllegalJarException {
		InstructionList il = method.getInstructionList();
		InstructionHandle start = il.getStart();

		// the call to the method that sets caller and balance cannot be put at the
		// beginning of the method, always: for constructors, Java bytecode requires
		// that their code starts with a call to a constructor of the superclass
		InstructionHandle where;
		boolean isConstructorOfInstanceInnerClass;
		boolean superconstructorIsFromContract;
		boolean superconstructorIsPayable;

		if (Const.CONSTRUCTOR_NAME.equals(method.getName())) {
			isConstructorOfInstanceInnerClass = isConstructorOfInstanceInnerClass();
			InstructionHandle callToSuperConstructor = callToSuperConstructor(method, il, slotForCaller, isConstructorOfInstanceInnerClass);
			if (!(callToSuperConstructor.getInstruction() instanceof INVOKESPECIAL invokespecial))
				throw new IllegalJarException("Expected invokespecial to call a superclass' constructor");

			// if the superconstructor is @FromContract, then it will take care of setting the caller for us
			String classNameOfSuperConstructor = invokespecial.getClassName(cpg);
			Type[] argumentTypes = invokespecial.getArgumentTypes(cpg);
			if (argumentTypes.length > 0 && DUMMY_OT.equals(argumentTypes[argumentTypes.length - 1])) {
				// the target has been already instrumented, we removed the extra arguments
				var copy = new Type[argumentTypes.length - 2];
				System.arraycopy(argumentTypes, 0, copy, 0, copy.length);
				argumentTypes = copy;
			}

			try {
				superconstructorIsFromContract = annotations.isFromContract(classNameOfSuperConstructor, Const.CONSTRUCTOR_NAME, argumentTypes, Type.VOID);
				superconstructorIsPayable = annotations.isPayable(classNameOfSuperConstructor, Const.CONSTRUCTOR_NAME, argumentTypes, Type.VOID);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}

			where = callToSuperConstructor.getNext();
			if (where == null)
				throw new IllegalJarException("Unexpected end of method");
		}
		else {
			isConstructorOfInstanceInnerClass = false;
			superconstructorIsFromContract = false;
			superconstructorIsPayable = false;
			where = start;
		}

		if (isPayable) {
			if (superconstructorIsPayable) {
				if (callerContract != classLoader.getContract()) {
					il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
					il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));
					il.insert(InstructionConst.POP);
				}
			}
			else {
				il.insert(start, InstructionFactory.createThis());
				il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));

				if (callerContract != classLoader.getContract())
					il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));

				il.insert(start, InstructionFactory.createLoad(DUMMY_OT, slotForCaller + 1));

				// a payable from contract method can have a first argument of type int/long/BigInteger
				Type amountType = method.getArgumentType(isConstructorOfInstanceInnerClass ? 1 : 0);
				il.insert(start, InstructionFactory.createLoad(amountType, isConstructorOfInstanceInnerClass ? 2 : 1));
				Type[] payableFromContractArgs = new Type[] { OBJECT_OT, OBJECT_OT, DUMMY_OT, amountType };
				il.insert(where, factory.createInvoke(WhitelistingConstants.RUNTIME_NAME,
					InstrumentationConstants.PAYABLE_FROM_CONTRACT, Type.VOID, payableFromContractArgs, Const.INVOKESTATIC));
			}
		}
		else if (!superconstructorIsFromContract) {
			il.insert(start, InstructionFactory.createThis());
			il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));

			if (callerContract != classLoader.getContract())
				il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));

			il.insert(where, factory.createInvoke(WhitelistingConstants.RUNTIME_NAME,
				InstrumentationConstants.FROM_CONTRACT, Type.VOID, FROM_CONTRACT_ARGS, Const.INVOKESTATIC));
		}
		else if (callerContract != classLoader.getContract()) {
			il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
			il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));
			il.insert(InstructionConst.POP);
		}
	}

	private boolean isConstructorOfInstanceInnerClass() {
		int dollarPos = className.lastIndexOf('$');
		Type[] methodArgs;

		// constructors of inner classes c have a first implicit parameter whose type t is the parent class
		// and they start with aload_0 aload_1 putfield c.f:t
		if (dollarPos > 0
			&& (methodArgs = method.getArgumentTypes()).length > 0 && methodArgs[0] instanceof ObjectType t
			&& t.getClassName().equals(className.substring(0, dollarPos))) {

			InstructionList il = method.getInstructionList();
			if (il != null && il.getLength() >= 3) {
				Instruction[] instructions = il.getInstructions();

				return instructions[0] instanceof LoadInstruction li0 && li0.getIndex() == 0
					&& instructions[1] instanceof LoadInstruction li1 && li1.getIndex() == 1
					&& instructions[2] instanceof PUTFIELD putfield && putfield.getFieldType(cpg).equals(t)
					&& putfield.getReferenceType(cpg) instanceof ObjectType ot && ot.getClassName().equals(className);
			}
		}

		return false;
	}

	/**
	 * From contract code call {@link io.takamaka.code.lang.Storage#fromContract(io.takamaka.code.lang.Contract)} or
	 * {@link io.takamaka.code.lang.Contract#payableFromContract(io.takamaka.code.lang.Contract, BigInteger)} at their
	 * beginning, to set the caller and the balance of the called code. In general,
	 * such call can be placed at the very beginning of the code. The only problem
	 * is related to constructors, that require (by JVM constraints)
	 * their code to start with a call to a
	 * constructor of their superclass. In that case, this method finds that call:
	 * after that, we can add the call that sets caller and balance.
	 * 
	 * @param constructor the {@code @@FromContract} constructor
	 * @param il the list of instructions of the {@code constructor}
	 * @param slotForCaller the local where the caller is passed to {@code constructor}
	 * @param isConstructorOfInstanceInnerClass true if and only if {@code constructor} belongs to an instance inner class
	 * @return the instruction before which the code that sets caller and balance can be placed
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 */
	private InstructionHandle callToSuperConstructor(MethodGen constructor, InstructionList il, int slotForCaller, boolean isConstructorOfInstanceInnerClass) throws IllegalJarException {
		InstructionHandle start = il.getStart();

		// we skip the initial aload_0 aload_1 putfield this$0
		if (isConstructorOfInstanceInnerClass) {
			if (il.getLength() <= 3)
				throw new IllegalJarException("Unexpectedly short constructor code");

			start = il.getInstructionHandles()[3];
		}

		// we have to identify the call to the constructor of the superclass:
		// the code of a constructor normally starts with an aload_0 whose value is consumed
		// by a call to a constructor of the superclass. In the middle, slotForCaller is not expected
		// to be modified. Note that this is the normal situation, as results from a normal
		// Java compiler. In principle, the Java bytecode might instead do very weird things,
		// including calling two constructors of the superclass at different places. In all such cases
		// this method fails and rejects the code: such non-standard code is not supported by Takamaka
		Instruction startInstruction = start.getInstruction();
		if (startInstruction instanceof LoadInstruction li && li.getIndex() == 0) {
			InstructionHandle callsForConstructorChaining = null;
			var seed = new HeightAtBytecode(start.getNext(), 1);
			var seen = new HashSet<HeightAtBytecode>();
			seen.add(seed);
			var workingSet = new ArrayList<HeightAtBytecode>();
			workingSet.add(seed);

			do {
				HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
				int stackHeightAfterBytecode = current.stackHeightAfterBytecode;
				Instruction bytecode = current.ih.getInstruction();

				if (bytecode instanceof StoreInstruction si) {
					int modifiedLocal = si.getIndex();
					int size = si.getType(cpg).getSize();
					if (modifiedLocal == slotForCaller || (size == 2 && modifiedLocal == slotForCaller - 1))
						throw new IllegalJarException("Unexpected modification of local " + slotForCaller + " before the initialization of " + className);
				}

				stackHeightAfterBytecode += bytecode.produceStack(cpg);
				stackHeightAfterBytecode -= bytecode.consumeStack(cpg);

				if (stackHeightAfterBytecode == 0) {
					// found a consumer of the aload_0: is it really a call to a constructor of the superclass or of the same class?
					if (bytecode instanceof INVOKESPECIAL invokespecial
							&& (invokespecial.getClassName(cpg).equals(getSuperclassName()) || invokespecial.getClassName(cpg).equals(className))
							&& Const.CONSTRUCTOR_NAME.equals(invokespecial.getMethodName(cpg))) {
						if (callsForConstructorChaining == null)
							callsForConstructorChaining = current.ih;
						else
							throw new IllegalJarException("Cannot identify a unique call to constructor chaining inside a constructor ot " + className);
					}
					else
						throw new IllegalJarException("Unexpected consumer of local 0 " + bytecode + " before initialization of " + className);
				}
				else if (bytecode instanceof GotoInstruction gi) {
					var added = new HeightAtBytecode(gi.getTarget(), stackHeightAfterBytecode);
					if (seen.add(added))
						workingSet.add(added);
				}
				else if (bytecode instanceof IfInstruction ii) {
					var added = new HeightAtBytecode(current.ih.getNext(), stackHeightAfterBytecode);
					if (seen.add(added))
						workingSet.add(added);

					added = new HeightAtBytecode(ii.getTarget(), stackHeightAfterBytecode);
					if (seen.add(added))
						workingSet.add(added);
				}
				else if (bytecode instanceof BranchInstruction || bytecode instanceof ATHROW || bytecode instanceof RETURN || bytecode instanceof RET)
					throw new IllegalJarException("Unexpected instruction " + bytecode + " before initialization of " + className);
				else {
					var added = new HeightAtBytecode(current.ih.getNext(), stackHeightAfterBytecode);
					if (seen.add(added))
						workingSet.add(added);
				}

				if (seen.size() > MAX_BYTECODES)
					throw new IllegalJarException("The lookup for the call to the superconstructor is too complex: I give up");
			}
			while (!workingSet.isEmpty());

			if (callsForConstructorChaining != null)
				return callsForConstructorChaining;
			else
				throw new IllegalJarException("Cannot identify any call to constructor chaining inside a constructor ot " + className);
		}
		else
			throw new IllegalJarException("Constructor of " + className + " does not start with aload 0");
	}

	/**
	 * Adds two extra caller parameters to the given method annotated as {@code @@FromContract}.
	 * 
	 * @param method the method
	 * @return the local variable used for the first extra parameter
	 */
	private int addExtraParameters(MethodGen method) {
		var args = new ArrayList<Type>();
		int slotsForParameters = 0;
		for (Type arg: method.getArgumentTypes()) {
			args.add(arg);
			slotsForParameters += arg.getSize();
		}
		args.add(CONTRACT_OT);

		args.add(DUMMY_OT); // to avoid name clashes after the addition
		method.setArgumentTypes(args.toArray(Type.NO_ARGS));

		String[] names = method.getArgumentNames();
		if (names != null) {
			List<String> namesAsList = new ArrayList<>();
			for (String name: names)
				namesAsList.add(name);
			namesAsList.add("caller");
			namesAsList.add("unused");
			method.setArgumentNames(namesAsList.toArray(String[]::new));
		}

		return slotsForParameters + 1;
	}

	private static class HeightAtBytecode {
		private final InstructionHandle ih;
		private final int stackHeightAfterBytecode;

		private HeightAtBytecode(InstructionHandle ih, int stackHeightAfterBytecode) throws IllegalJarException {
			if (ih == null)
				throw new IllegalJarException("Unexpected end of code");

			if (stackHeightAfterBytecode < 0)
				throw new IllegalJarException("Unexpected negative stack height");

			this.ih = ih;
			this.stackHeightAfterBytecode = stackHeightAfterBytecode;
		}

		@Override
		public String toString() {
			return ih + " with " + stackHeightAfterBytecode + " stack elements";
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof HeightAtBytecode hab && hab.ih == ih
				&& hab.stackHeightAfterBytecode == stackHeightAfterBytecode;
		}

		@Override
		public int hashCode() {
			return ih.getPosition() ^ stackHeightAfterBytecode;
		}
	}
}