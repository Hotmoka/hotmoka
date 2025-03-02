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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.hotmoka.verification.PushersIterators;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.whitelisting.WhitelistingConstants;
import io.takamaka.code.constants.Constants;

/**
 * Passes the trailing implicit parameters to calls to methods annotated as {@code @@FromContract}.
 * They are the caller, the payer of the callee and {@code null} (as a dummy argument).
 */
public class AddExtraArgsToCallsToFromContract extends MethodLevelInstrumentation {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType RUNTIME_OT = new ObjectType(WhitelistingConstants.RUNTIME_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(WhitelistingConstants.DUMMY_NAME);

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 * @param method the method being instrumented
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 */
	public AddExtraArgsToCallsToFromContract(InstrumentedClassImpl.Builder builder, MethodGen method) throws IllegalJarException {
		builder.super(method);

		if (!method.isAbstract()) {
			InstructionList il = method.getInstructionList();

			// we need a support list since the instrumentation will modify the il we are iterating upon
			// and a deep copy would be too expensive
			List<InstructionHandle> callsToFromContract = new ArrayList<>();
			for (var ih: il)
				if (isCallToFromContract(ih.getInstruction()))
					callsToFromContract.add(ih);

			for (InstructionHandle ih: callsToFromContract)
				passExtraArgsToCallToFromContract(il, ih);
		}
	}

	/**
	 * Passes the trailing implicit parameters to the given call to a {@code @@FromContract}. They
	 * are the caller, the payer (if any) and {@code null} (for the dummy argument).
	 * 
	 * @param il the instructions of the method being instrumented
	 * @param ih the call to the entry
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 */
	private void passExtraArgsToCallToFromContract(InstructionList il, InstructionHandle ih) throws IllegalJarException {
		var invoke = (InvokeInstruction) ih.getInstruction(); // true since ih comes from callsToFromContract above
		var args = invoke.getArgumentTypes(cpg);
		String methodName = invoke.getMethodName(cpg);
		var returnType = invoke.getReturnType(cpg);
		int slots = Stream.of(args).mapToInt(Type::getSize).sum();
		
		if (invoke instanceof INVOKEDYNAMIC invokedynamic) {
			if (!(cpg.getConstant(invokedynamic.getIndex()) instanceof ConstantInvokeDynamic cid))
				throw new IllegalJarException("Illegal constant");

			// this is an invokedynamic that calls a @FromContract: we must capture the calling contract
			var expandedArgs = new Type[args.length + 1];
			System.arraycopy(args, 0, expandedArgs, 1, args.length);
			expandedArgs[0] = new ObjectType(className);
			var expandedCid = new ConstantInvokeDynamic(cid.getBootstrapMethodAttrIndex(),
				cpg.addNameAndType(methodName, Type.getMethodSignature(returnType, expandedArgs)));
			int index = addInvokeDynamicToConstantPool(expandedCid);
			var copied = (INVOKEDYNAMIC) invokedynamic.copy();
			copied.setIndex(index);
			ih.setInstruction(copied);

			// we park the arguments of the invokedynamic into new local variables
			int usedLocals = method.getMaxLocals();
			int offset = slots;
			for (int pos = args.length - 1; pos >= 0; pos--) {
				offset -= args[pos].getSize();
				il.insert(ih, InstructionFactory.createStore(args[pos], usedLocals + offset));
			}

			// we added the first, extra parameter (the caller)
			il.insert(ih, InstructionConst.ALOAD_0);

			// we push back the previous arguments of the invokedynamic
			offset = 0;
			for (var arg: args) {
				il.insert(ih, InstructionFactory.createLoad(arg, usedLocals + offset));
				offset += arg.getSize();
			}
		}
		else {
			var expandedArgs = new Type[args.length + 2];
			System.arraycopy(args, 0, expandedArgs, 0, args.length);
			expandedArgs[args.length] = CONTRACT_OT;
			expandedArgs[args.length + 1] = DUMMY_OT;

			boolean onThis = pusherIsLoad0(ih, slots + 1, method);

			if (onThis) {
				// the call is on "this": it inherits our caller
				var ourArgs = method.getArgumentTypes();
				boolean isFromContract;

				try {
					isFromContract = annotations.isFromContract(className, method.getName(), ourArgs, method.getReturnType());
				}
				catch (ClassNotFoundException e) {
					throw new IllegalJarException(e);
				}

				if (isFromContract) {
					int ourArgsSlots = Stream.of(ourArgs).mapToInt(Type::getSize).sum();
					// the call is inside a @FromContract: its last minus one argument is the caller: we pass it
					ih.setInstruction(new LoadCaller(ourArgsSlots + 1));
				}
				else {
					// the call must be inside a lambda that is part of a @FromContract: since it has no caller argument,
					// we must call this.caller() and pass its return value as caller for the target method
					ih.setInstruction(InstructionConst.ALOAD_0);
					ih = il.append(ih, factory.createInvoke(Constants.STORAGE_NAME, InstrumentationConstants.CALLER, CONTRACT_OT, Type.NO_ARGS, Const.INVOKESPECIAL));
				}

				il.append(ih, factory.createInvoke(invoke.getClassName(cpg), methodName, returnType, expandedArgs, invoke.getOpcode()));
				if (Const.CONSTRUCTOR_NAME.equals(methodName))
					il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
				else
					// otherwise we pass a tag that states that it is a method called on "this"
					il.append(ih, factory.createGetStatic(WhitelistingConstants.DUMMY_NAME, "METHOD_ON_THIS", DUMMY_OT));
			}
			else {
				// the call is not on "this": it must be inside a contract "this" that becomes the caller
				ih.setInstruction(InstructionConst.ALOAD_0);
				il.append(ih, factory.createInvoke(invoke.getClassName(cpg), methodName, returnType, expandedArgs, invoke.getOpcode()));
				il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
			}
		}
	}

	private boolean pusherIsLoad0(InstructionHandle ih, int slots, MethodGen method) throws IllegalJarException {
		var it = PushersIterators.of(ih, slots, method);

		while (it.hasNext())
			if (!(it.next().getInstruction() instanceof LoadInstruction load) || load.getIndex() != 0)
				return false;

		return true;
	}

	/**
	 * An ALOAD instruction that is used to load the calling contract.
	 * This allows us later to distinguish the instruction from a normal ALOAD.
	 */
	static class LoadCaller extends ALOAD {
		private LoadCaller(int n) {
			super(n);
		}
	}

	/**
	 * Determines if the given instruction calls a method annotated as {@code @@FromContract}.
	 * 
	 * @param instruction the instruction
	 * @return true if and only if that condition holds
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 */
	private boolean isCallToFromContract(Instruction instruction) throws IllegalJarException {
		if (instruction instanceof INVOKEDYNAMIC invokedynamic)
			return bootstrapMethodsThatWillRequireExtraThis.contains(bootstraps.getBootstrapFor(invokedynamic));
		else if (instruction instanceof InvokeInstruction invoke &&
				// we do not consider calls added by instrumentation
				invoke.getReferenceType(cpg) instanceof ObjectType ot && !RUNTIME_OT.equals(ot)) {

			try {
				return annotations.isFromContract(ot.getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}
		else
			return false;
	}

	/**
	 * BCEL does not (yet?) provide a method to add an invokedynamic constant into a
	 * constant pool. Hence we have to rely to a trick: first we add a new integer
	 * constant to the constant pool; then we replace it with the invokedynamic
	 * constant. Ugly, but it currently seem to be the only way.
	 * 
	 * @param cid the constant to add
	 * @return the index at which the constant has been added
	 */
	private int addInvokeDynamicToConstantPool(ConstantInvokeDynamic cid) {
		// first we check if an equal constant method handle was already in the constant pool
		int size = cpg.getSize(), index;
		for (index = 0; index < size; index++)
			if (cpg.getConstant(index) instanceof ConstantInvokeDynamic cid2
					&& cid2.getBootstrapMethodAttrIndex() == cid.getBootstrapMethodAttrIndex() && cid2.getNameAndTypeIndex() == cid.getNameAndTypeIndex())
				return index; // found

		// otherwise, we first add an integer that was not already there
		int counter = 0;
		do {
			index = cpg.addInteger(counter++);
		}
		while (cpg.getSize() == size);

		// and finally replace the integer constant with the method handle constant
		cpg.setConstant(index, cid);

		return index;
	}
}