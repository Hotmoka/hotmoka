/*
Copyright 2019 Fausto Spoto (fausto.spoto@univr.it)

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

package it.univr.bcel.internal;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DSTORE;
import org.apache.bcel.generic.FSTORE;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LSTORE;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;
import org.apache.bcel.verifier.statics.DOUBLE_Upper;
import org.apache.bcel.verifier.statics.LONG_Upper;

import it.univr.bcel.TypeInferenceException;
import it.univr.bcel.Types;
import it.univr.bcel.UninitializedObjectType;

/**
 * The types at an instruction. Instances of this class specify the type
 * of each single stack element and local variable at the instruction.
 * Instances of this class are immutable.
 * 
 * @author Fausto Spoto <fausto.spoto@univr.it>
 */
class TypesImpl implements Types {
	private final Type[] stack;
	private final Type[] locals;
	private final static Type[] NO_TYPES = new Type[0];

	/**
	 * Creates the types at the beginning of the given method or constructor.
	 * 
	 * @param methodOrConstructor the method or constructor
	 */
	TypesImpl(MethodGen methodOrConstructor) {
		Type[] args = methodOrConstructor.getArgumentTypes();
		int localsCount = Stream.of(args).mapToInt(Type::getSize).sum();
		if (!methodOrConstructor.isStatic())
			localsCount++;

		this.locals = new Type[localsCount];
		this.stack = NO_TYPES;

		int pos = 0;
		if (methodOrConstructor.getName().equals(Const.CONSTRUCTOR_NAME))
			locals[pos++] = new UninitializedObjectTypeImpl(new ObjectType(methodOrConstructor.getClassName()));
		else if (!methodOrConstructor.isStatic())
			locals[pos++] = new ObjectType(methodOrConstructor.getClassName());

		for (Type arg: methodOrConstructor.getArgumentTypes()) {
			locals[pos++] = arg.normalizeForStackOrLocal();
			if (arg.equals(Type.DOUBLE))
				locals[pos++] = DOUBLE_Upper.theInstance();
			else if (arg.equals(Type.LONG))
				locals[pos++] = LONG_Upper.theInstance();
		}
	}

	/**
	 * Creates the types with the given stack elements and local variables types.
	 * 
	 * @param stack the stack elements types
	 */
	private TypesImpl(Type[] locals, Type[] stack) {
		this.stack = stack;
		this.locals = locals;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TypesImpl
			&& Arrays.equals(locals, ((TypesImpl) other).locals)
			&& Arrays.equals(stack, ((TypesImpl) other).stack);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(stack) ^ Arrays.hashCode(locals);
	}

	@Override
	public String toString() {
		return "locals: " + Arrays.toString(locals) + ", stack: " + Arrays.toString(stack);
	}

	@Override
	public int localsCount() {
		return locals.length;
	}

	@Override
	public int stackHeight() {
		return stack.length;
	}

	@Override
	public Type getStack(int pos) {
		return stack[pos];
	}

	@Override
	public Type getLocal(int pos) {
		return locals[pos];
	}

	@Override
	public Stream<Type> getStack() {
		return Stream.of(stack);
	}

	@Override
	public Stream<Type> getLocals() {
		return Stream.of(locals);
	}

	@Override
	public Stream<Type> getStackOnlyFirstSlots() {
		return Stream.of(stack).filter(type -> type != DOUBLE_Upper.theInstance() && type != LONG_Upper.theInstance());
	}

	@Override
	public Stream<Type> getLocalsOnlyFirstSlots() {
		return Stream.of(locals).filter(type -> type != DOUBLE_Upper.theInstance() && type != LONG_Upper.theInstance());
	}

	/**
	 * Yields new types where the given object is now considered as initialized.
	 * 
	 * @param initialized the object that gets initialized
	 * @return the resulting types
	 */
	private TypesImpl initialize(UninitializedObjectTypeImpl initialized) {
		Type[] stack = this.stack.clone();
		Type[] locals = this.locals.clone();

		for (int pos = 0; pos < stack.length; pos++)
			if (stack[pos].equals(initialized))
				stack[pos] = initialized.onceInitialized();

		for (int pos = 0; pos < locals.length; pos++)
			if (locals[pos].equals(initialized))
				locals[pos] = initialized.onceInitialized();

		return new TypesImpl(locals, stack);
	}

	private static Type leastCommonSupertype(Type type1, Type type2) {
		if (type1 == type2 || type1.equals(type2))
			return type1;
		else if (type1 == Type.UNKNOWN || type2 == Type.UNKNOWN)
			return Type.UNKNOWN;
		if (type1 == Type.NULL)
			return type2;
		else if (type2 == Type.NULL)
			return type1;
		else if (type1 instanceof UninitializedObjectType)
			if (type1.equals(type2))
				return type1;
			else
				throw new TypeInferenceException("Cannot merge " + type1 + " with " + type2);
		else if (type2 instanceof UninitializedObjectType)
			if (type2.equals(type1))
				return type2;
			else
				throw new TypeInferenceException("Cannot merge " + type1 + " with " + type2);
		else if (type1 instanceof BasicType || type2 instanceof BasicType
				|| type1 == DOUBLE_Upper.theInstance() || type2 == DOUBLE_Upper.theInstance()
				|| type1 == LONG_Upper.theInstance() || type2 == LONG_Upper.theInstance())
			if (type1.equals(type2))
				return type1;
			else
				return Type.UNKNOWN;
		else {
			try {
				Type result = ((ReferenceType) type1).getFirstCommonSuperclass((ReferenceType) type2);
				if (result == null)
					throw new TypeInferenceException("Cannot merge " + type1 + " with " + type2);
				else
					return result;
			}
			catch (ClassNotFoundException e) {
				throw new TypeInferenceException("Cannot merge " + type1 + " with " + type2 + " since " + e);
			}
		}
	}

	private static Type[] mergeStack(Type[] stack1, Type[] stack2) {
		int size = stack1.length;
		if (size != stack2.length)
			throw new TypeInferenceException("Stack height mismatch " + size + " vs " + stack2.length);
		else if (Arrays.equals(stack1, stack2))
			return stack1;
		else {
			Type[] result = new Type[size];
			for (int pos = 0; pos < size; pos++)
				result[pos] = leastCommonSupertype(stack1[pos], stack2[pos]);

			return result;
		}
	}

	private static Type[] mergeLocals(Type[] locals1, Type[] locals2) {
		if (Arrays.equals(locals1, locals2))
			return locals1;
		else {
			int size = Math.min(locals1.length, locals2.length);
			Type[] result = new Type[size];
			for (int pos = 0; pos < size; pos++)
				result[pos] = leastCommonSupertype(locals1[pos], locals2[pos]);

			return result;
		}
	}

	private TypesImpl pop() {
		Type[] stack = new Type[this.stack.length - 1];
		System.arraycopy(this.stack, 0, stack, 0, stack.length);
		return new TypesImpl(locals, stack);
	}

	private TypesImpl pop2() {
		Type[] stack = new Type[this.stack.length - 2];
		System.arraycopy(this.stack, 0, stack, 0, stack.length);
		return new TypesImpl(locals, stack);
	}

	private TypesImpl pop3() {
		Type[] stack = new Type[this.stack.length - 3];
		System.arraycopy(this.stack, 0, stack, 0, stack.length);
		return new TypesImpl(locals, stack);
	}

	private TypesImpl pop4() {
		Type[] stack = new Type[this.stack.length - 4];
		System.arraycopy(this.stack, 0, stack, 0, stack.length);
		return new TypesImpl(locals, stack);
	}

	private TypesImpl pop(int slots) {
		Type[] stack = new Type[this.stack.length - slots];
		System.arraycopy(this.stack, 0, stack, 0, stack.length);
		return new TypesImpl(locals, stack);
	}

	private TypesImpl popThenPush(Type type) {
		int thisStackLength = this.stack.length;
		Type[] stack = new Type[thisStackLength - 1 + type.getSize()];
		System.arraycopy(this.stack, 0, stack, 0, thisStackLength - 1);
		stack[thisStackLength - 1] = type;
		if (type.equals(Type.DOUBLE))
			stack[thisStackLength] = DOUBLE_Upper.theInstance();
		else if (type.equals(Type.LONG))
			stack[thisStackLength] = LONG_Upper.theInstance();
	
		return new TypesImpl(locals, stack);
	}

	private TypesImpl pop2ThenPush(Type type) {
		int thisStackLength = this.stack.length;
		Type[] stack = new Type[thisStackLength - 2 + type.getSize()];
		System.arraycopy(this.stack, 0, stack, 0, thisStackLength - 2);
		stack[thisStackLength - 2] = type;
		if (type.equals(Type.DOUBLE))
			stack[thisStackLength - 1] = DOUBLE_Upper.theInstance();
		else if (type.equals(Type.LONG))
			stack[thisStackLength - 1] = LONG_Upper.theInstance();

		return new TypesImpl(locals, stack);
	}

	private TypesImpl pop4ThenPush(Type type) {
		int thisStackLength = this.stack.length;
		Type[] stack = new Type[thisStackLength - 4 + type.getSize()];
		System.arraycopy(this.stack, 0, stack, 0, thisStackLength - 4);
		stack[thisStackLength - 4] = type;
		if (type.equals(Type.DOUBLE))
			stack[thisStackLength - 3] = DOUBLE_Upper.theInstance();
		else if (type.equals(Type.LONG))
			stack[thisStackLength - 3] = LONG_Upper.theInstance();

		return new TypesImpl(locals, stack);
	}

	private TypesImpl popThenPush(int slots, Type type) {
		int thisStackLength = this.stack.length;
		Type[] stack = new Type[thisStackLength - slots + type.getSize()];
		System.arraycopy(this.stack, 0, stack, 0, thisStackLength - slots);
		stack[thisStackLength - slots] = type;
		if (type.equals(Type.DOUBLE))
			stack[thisStackLength - slots + 1] = DOUBLE_Upper.theInstance();
		else if (type.equals(Type.LONG))
			stack[thisStackLength - slots + 1] = LONG_Upper.theInstance();

		return new TypesImpl(locals, stack);
	}

	private TypesImpl push(Type type) {
		int thisStackLength = this.stack.length;
		Type[] stack = new Type[thisStackLength + type.getSize()];
		System.arraycopy(this.stack, 0, stack, 0, thisStackLength);
		stack[thisStackLength] = type;
		if (type.equals(Type.DOUBLE))
			stack[thisStackLength + 1] = DOUBLE_Upper.theInstance();
		else if (type.equals(Type.LONG))
			stack[thisStackLength + 1] = LONG_Upper.theInstance();

		return new TypesImpl(locals, stack);
	}

	/**
	 * Yields the merge of these types with the other types.
	 * They must have the same number of stack elements. Local variables
	 * may be different in size, in which case the minimum length is
	 * yielded in the result. At each given stack position or local variable
	 * that exist in both types, the least common supertype is computed
	 * and put in the resulting merged types.
	 * 
	 * @param other the other types
	 * @return the merge of this and {@code other}
	 */
	TypesImpl merge(TypesImpl other) {
		return this.equals(other) ? this : new TypesImpl(mergeLocals(locals, other.locals), mergeStack(stack, other.stack));
	}

	/**
	 * Yields the types resulting from a successful execution of the
	 * given instruction from these types.
	 * 
	 * @param ih the instruction
	 * @param cpg the constant pool of the class where the instruction occurs
	 * @return the resulting types
	 */
	TypesImpl after(InstructionHandle ih, ConstantPoolGen cpg) {
		Instruction ins = ih.getInstruction();
		switch (ins.getOpcode()) {
		case Const.AALOAD:
			Type array = stack[stack.length - 2];
			if (array.equals(Type.NULL))
				return pop(); // result is null, what else?
			else
				return pop2ThenPush(((ArrayType) array).getElementType());
		case Const.AASTORE:
		case Const.BASTORE:
		case Const.CASTORE:
		case Const.FASTORE:
		case Const.IASTORE:
			return pop3();
		case Const.ACONST_NULL:
			return push(Type.NULL);
		case Const.ALOAD:
		case Const.ALOAD_0:
		case Const.ALOAD_1:
		case Const.ALOAD_2:
		case Const.ALOAD_3:
			return push(locals[((ALOAD) ins).getIndex()]);
		case Const.ANEWARRAY:
			return popThenPush(new ArrayType(((ANEWARRAY) ins).getType(cpg), 1));
		case Const.ARRAYLENGTH:
			return popThenPush(BasicType.INT);
		case Const.ASTORE:
		case Const.ASTORE_0:
		case Const.ASTORE_1:
		case Const.ASTORE_2:
		case Const.ASTORE_3: {
			int index = ((ASTORE) ins).getIndex();
			Type typeStored = stack[stack.length - 1];
			if (index < locals.length && locals[index].equals(typeStored))
				return pop(); // optimization
	
			Type[] locals;
			if (index < this.locals.length)
				locals = this.locals.clone();
			else {
				locals = new Type[index + 1];
				System.arraycopy(this.locals, 0, locals, 0, this.locals.length);
				for (int pos = this.locals.length; pos < index; pos++)
					locals[pos] = Type.UNKNOWN;
			}
	
			if (locals[index] == DOUBLE_Upper.theInstance() || locals[index] == LONG_Upper.theInstance())
				locals[index - 1] = Type.UNKNOWN;
			else if (BasicType.DOUBLE.equals(locals[index]) || BasicType.LONG.equals(locals[index]))
				locals[index + 1] = Type.UNKNOWN;
	
			locals[index] = typeStored;
	
			Type[] stack = new Type[this.stack.length - 1];
			System.arraycopy(this.stack, 0, stack, 0, stack.length);
			return new TypesImpl(locals, stack);
		}
		case Const.BALOAD:
		case Const.CALOAD:
			return pop2ThenPush(BasicType.INT);
		case Const.BIPUSH:
		case Const.SIPUSH:
			return push(BasicType.INT);
		case Const.CHECKCAST:
			return popThenPush(((CHECKCAST) ins).getType(cpg));
		case Const.D2F:
			return pop2ThenPush(BasicType.FLOAT);
		case Const.D2I:
			return popThenPush(BasicType.INT);
		case Const.D2L:
			return popThenPush(BasicType.LONG);
		case Const.DADD:
		case Const.DDIV:
		case Const.DMUL:
		case Const.DREM:
		case Const.DSUB:
			return pop2();
		case Const.DALOAD:
			return pop2ThenPush(BasicType.DOUBLE);
		case Const.DASTORE:
			return pop4();
		case Const.DCMPG:
		case Const.DCMPL:
			return pop4ThenPush(BasicType.INT);
		case Const.DCONST_0:
		case Const.DCONST_1:
		case Const.DLOAD:
		case Const.DLOAD_0:
		case Const.DLOAD_1:
		case Const.DLOAD_2:
		case Const.DLOAD_3:
			return push(BasicType.DOUBLE);
		case Const.DNEG:
		case Const.FNEG:
		case Const.INEG:
		case Const.LNEG:
			return this;
		case Const.DSTORE:
		case Const.DSTORE_0:
		case Const.DSTORE_1:
		case Const.DSTORE_2:
		case Const.DSTORE_3: {
			int index = ((DSTORE) ins).getIndex();
			if (index < locals.length - 1 && locals[index].equals(BasicType.DOUBLE))
				return pop2(); // optimization
	
			Type[] locals;
			if (index < this.locals.length - 1)
				locals = this.locals.clone();
			else {
				locals = new Type[index + 2];
				System.arraycopy(this.locals, 0, locals, 0, this.locals.length);
				for (int pos = this.locals.length; pos < index; pos++)
					locals[pos] = Type.UNKNOWN;
			}
	
			if (locals[index] == DOUBLE_Upper.theInstance() || locals[index] == LONG_Upper.theInstance())
				locals[index - 1] = Type.UNKNOWN;
			else if (BasicType.DOUBLE.equals(locals[index + 1]) || BasicType.LONG.equals(locals[index + 1]))
				locals[index + 2] = Type.UNKNOWN;
	
			locals[index] = BasicType.DOUBLE;
			locals[index + 1] = DOUBLE_Upper.theInstance();
	
			Type[] stack = new Type[this.stack.length - 2];
			System.arraycopy(this.stack, 0, stack, 0, stack.length);
			return new TypesImpl(locals, stack);
		}
		case Const.DUP:
			return push(stack[stack.length - 1]);
		case Const.DUP_X1: {
			int thisStackLength = this.stack.length;
			Type[] stack = new Type[thisStackLength + 1];
			System.arraycopy(this.stack, 0, stack, 0, thisStackLength - 2);
			stack[thisStackLength - 2] = stack[thisStackLength] = this.stack[thisStackLength - 1];
			stack[thisStackLength - 1] = this.stack[thisStackLength - 2];
			return new TypesImpl(locals, stack);
		}
		case Const.DUP_X2: {
			int thisStackLength = this.stack.length;
			Type[] stack = new Type[thisStackLength + 1];
			System.arraycopy(this.stack, 0, stack, 0, thisStackLength - 3);
			stack[thisStackLength - 3] = stack[thisStackLength] = this.stack[thisStackLength - 1];
			stack[thisStackLength - 1] = this.stack[thisStackLength - 2];
			stack[thisStackLength - 2] = this.stack[thisStackLength - 3];
			return new TypesImpl(locals, stack);
		}
		case Const.DUP2: {
			int thisStackLength = this.stack.length;
			Type[] stack = new Type[thisStackLength + 2];
			System.arraycopy(this.stack, 0, stack, 0, thisStackLength);
			stack[thisStackLength] = this.stack[thisStackLength - 2];
			stack[thisStackLength + 1] = this.stack[thisStackLength - 1];
			return new TypesImpl(locals, stack);
		}
		case Const.DUP2_X1: {
			int thisStackLength = this.stack.length;
			Type[] stack = new Type[thisStackLength + 2];
			System.arraycopy(this.stack, 0, stack, 0, thisStackLength - 3);
			stack[thisStackLength - 1] = this.stack[thisStackLength - 3];
			stack[thisStackLength - 3] = stack[thisStackLength] = this.stack[thisStackLength - 2];
			stack[thisStackLength - 2] = stack[thisStackLength + 1] = this.stack[thisStackLength - 1];
			return new TypesImpl(locals, stack);
		}
		case Const.DUP2_X2: {
			int thisStackLength = this.stack.length;
			Type[] stack = new Type[thisStackLength + 2];
			System.arraycopy(this.stack, 0, stack, 0, thisStackLength - 4);
			stack[thisStackLength - 2] = this.stack[thisStackLength - 4];
			stack[thisStackLength - 1] = this.stack[thisStackLength - 3];
			stack[thisStackLength - 4] = stack[thisStackLength] = this.stack[thisStackLength - 2];
			stack[thisStackLength - 3] = stack[thisStackLength + 1] = this.stack[thisStackLength - 1];
			return new TypesImpl(locals, stack);
		}
		case Const.F2D:
			return popThenPush(BasicType.DOUBLE);
		case Const.F2I:
			return popThenPush(BasicType.INT);
		case Const.F2L:
			return popThenPush(BasicType.LONG);
		case Const.FADD:
		case Const.FDIV:
		case Const.FMUL:
		case Const.FREM:
		case Const.FSUB:
			return pop();
		case Const.FALOAD:
			return pop2ThenPush(BasicType.FLOAT);
		case Const.FCMPG:
		case Const.FCMPL:
			return pop2ThenPush(BasicType.INT);
		case Const.FCONST_0:
		case Const.FCONST_1:
		case Const.FCONST_2:
		case Const.FLOAD:
		case Const.FLOAD_0:
		case Const.FLOAD_1:
		case Const.FLOAD_2:
		case Const.FLOAD_3:
			return push(BasicType.FLOAT);
		case Const.FSTORE:
		case Const.FSTORE_0:
		case Const.FSTORE_1:
		case Const.FSTORE_2:
		case Const.FSTORE_3: {
			int index = ((FSTORE) ins).getIndex();
			if (index < locals.length && locals[index].equals(BasicType.FLOAT))
				return pop(); // optimization
	
			Type[] locals;
			if (index < this.locals.length)
				locals = this.locals.clone();
			else {
				locals = new Type[index + 1];
				System.arraycopy(this.locals, 0, locals, 0, this.locals.length);
				for (int pos = this.locals.length; pos < index; pos++)
					locals[pos] = Type.UNKNOWN;
			}
	
			if (locals[index] == DOUBLE_Upper.theInstance() || locals[index] == LONG_Upper.theInstance())
				locals[index - 1] = Type.UNKNOWN;
			else if (BasicType.DOUBLE.equals(locals[index]) || BasicType.LONG.equals(locals[index]))
				locals[index + 1] = Type.UNKNOWN;
	
			locals[index] = BasicType.FLOAT;
	
			Type[] stack = new Type[this.stack.length - 1];
			System.arraycopy(this.stack, 0, stack, 0, stack.length);
			return new TypesImpl(locals, stack);
		}
		case Const.GETFIELD:
			return popThenPush(((TypedInstruction) ins).getType(cpg));
		case Const.GETSTATIC:
			return push(((TypedInstruction) ins).getType(cpg));
		case Const.GOTO:
		case Const.GOTO_W:
		case Const.I2B:
		case Const.I2C:
		case Const.I2S:
			return this;
		case Const.I2D:
			return popThenPush(BasicType.DOUBLE);
		case Const.I2F:
			return popThenPush(BasicType.FLOAT);
		case Const.I2L:
			return popThenPush(BasicType.LONG);
		case Const.IADD:
		case Const.IDIV:
		case Const.IMUL:
		case Const.IREM:
		case Const.ISUB:
		case Const.IAND:
		case Const.IOR:
		case Const.ISHL:
		case Const.ISHR:
		case Const.IUSHR:
		case Const.IXOR:
			return pop();
		case Const.IALOAD:
			return pop2ThenPush(BasicType.INT);
		case Const.ICONST_M1:
		case Const.ICONST_0:
		case Const.ICONST_1:
		case Const.ICONST_2:
		case Const.ICONST_3:
		case Const.ICONST_4:
		case Const.ICONST_5:
			return push(BasicType.INT);
		case Const.IF_ACMPEQ:
		case Const.IF_ACMPNE:
		case Const.IF_ICMPEQ:
		case Const.IF_ICMPGE:
		case Const.IF_ICMPGT:
		case Const.IF_ICMPLE:
		case Const.IF_ICMPLT:
		case Const.IF_ICMPNE:
			return pop2();
		case Const.IFEQ:
		case Const.IFGE:
		case Const.IFGT:
		case Const.IFLE:
		case Const.IFLT:
		case Const.IFNE:
		case Const.IFNONNULL:
		case Const.IFNULL:
			return pop();
		case Const.IINC:
			return this;
		case Const.ILOAD:
		case Const.ILOAD_0:
		case Const.ILOAD_1:
		case Const.ILOAD_2:
		case Const.ILOAD_3:
			return push(BasicType.INT);
		case Const.INSTANCEOF:
			return popThenPush(BasicType.INT);
		case Const.INVOKEDYNAMIC:
		case Const.INVOKESTATIC: {
			InvokeInstruction invoke = (InvokeInstruction) ins;
			int slotsForArgs = Stream.of(invoke.getArgumentTypes(cpg)).mapToInt(Type::getSize).sum();
			Type returnType = invoke.getReturnType(cpg);
			if (returnType == BasicType.VOID)
				return pop(slotsForArgs);
			else
				return popThenPush(slotsForArgs, returnType.normalizeForStackOrLocal());
		}
		case Const.INVOKESPECIAL: {
			InvokeInstruction invoke = (InvokeInstruction) ins;
			int slotsForArgs = 1 + Stream.of(invoke.getArgumentTypes(cpg)).mapToInt(Type::getSize).sum();
			Type receiver = stack[stack.length - slotsForArgs];
			boolean isInitializer = receiver instanceof UninitializedObjectTypeImpl;
			Type returnType = invoke.getReturnType(cpg);
			if (isInitializer)
				if (returnType == BasicType.VOID)
					return pop(slotsForArgs).initialize((UninitializedObjectTypeImpl) receiver);
				else
					return popThenPush(slotsForArgs, returnType.normalizeForStackOrLocal()).initialize((UninitializedObjectTypeImpl) receiver);
			else
				if (returnType == BasicType.VOID)
					return pop(slotsForArgs);
				else
					return popThenPush(slotsForArgs, returnType.normalizeForStackOrLocal());
		}
		case Const.INVOKEVIRTUAL:
		case Const.INVOKEINTERFACE: {
			InvokeInstruction invoke = (InvokeInstruction) ins;
			int slotsForArgs = 1 + Stream.of(invoke.getArgumentTypes(cpg)).mapToInt(Type::getSize).sum();
			Type returnType = invoke.getReturnType(cpg);
			if (returnType == BasicType.VOID)
				return pop(slotsForArgs);
			else
				return popThenPush(slotsForArgs, returnType.normalizeForStackOrLocal());
		}
		case Const.ISTORE:
		case Const.ISTORE_0:
		case Const.ISTORE_1:
		case Const.ISTORE_2:
		case Const.ISTORE_3: {
			int index = ((ISTORE) ins).getIndex();
			if (index < locals.length && locals[index].equals(BasicType.INT))
				return pop(); // optimization
	
			Type[] locals;
			if (index < this.locals.length)
				locals = this.locals.clone();
			else {
				locals = new Type[index + 1];
				System.arraycopy(this.locals, 0, locals, 0, this.locals.length);
				for (int pos = this.locals.length; pos < index; pos++)
					locals[pos] = Type.UNKNOWN;
			}
	
			if (locals[index] == DOUBLE_Upper.theInstance() || locals[index] == LONG_Upper.theInstance())
				locals[index - 1] = Type.UNKNOWN;
			else if (BasicType.DOUBLE.equals(locals[index]) || BasicType.LONG.equals(locals[index]))
				locals[index + 1] = Type.UNKNOWN;
	
			locals[index] = BasicType.INT;
	
			Type[] stack = new Type[this.stack.length - 1];
			System.arraycopy(this.stack, 0, stack, 0, stack.length);
			return new TypesImpl(locals, stack);
		}
		case Const.L2D:
			return pop2ThenPush(BasicType.DOUBLE);
		case Const.L2F:
			return pop2ThenPush(BasicType.FLOAT);
		case Const.L2I:
			return pop2ThenPush(BasicType.INT);
		case Const.LSTORE:
		case Const.LSTORE_0:
		case Const.LSTORE_1:
		case Const.LSTORE_2:
		case Const.LSTORE_3: {
			int index = ((LSTORE) ins).getIndex();
			if (index < locals.length - 1 && locals[index].equals(BasicType.LONG))
				return pop2(); // optimization
	
			Type[] locals;
			if (index < this.locals.length - 1)
				locals = this.locals.clone();
			else {
				locals = new Type[index + 2];
				System.arraycopy(this.locals, 0, locals, 0, this.locals.length);
				for (int pos = this.locals.length; pos < index; pos++)
					locals[pos] = Type.UNKNOWN;
			}
	
			if (locals[index] == DOUBLE_Upper.theInstance() || locals[index] == LONG_Upper.theInstance())
				locals[index - 1] = Type.UNKNOWN;
			else if (BasicType.DOUBLE.equals(locals[index + 1]) || BasicType.LONG.equals(locals[index + 1]))
				locals[index + 2] = Type.UNKNOWN;
	
			locals[index] = BasicType.LONG;
			locals[index + 1] = LONG_Upper.theInstance();
	
			Type[] stack = new Type[this.stack.length - 2];
			System.arraycopy(this.stack, 0, stack, 0, stack.length);
			return new TypesImpl(locals, stack);
		}
		case Const.LADD:
		case Const.LDIV:
		case Const.LMUL:
		case Const.LREM:
		case Const.LSUB:
		case Const.LAND:
		case Const.LOR:
		case Const.LXOR:
			return pop2();
		case Const.LALOAD:
			return pop2ThenPush(BasicType.LONG);
		case Const.LASTORE:
			return pop4();
		case Const.LCMP:
			return pop4ThenPush(BasicType.INT);
		case Const.LCONST_0:
		case Const.LCONST_1:
			return push(BasicType.LONG);
		case Const.LDC:
		case Const.LDC_W:
		case Const.LDC2_W:
			return push(((TypedInstruction) ins).getType(cpg));
		case Const.LLOAD:
		case Const.LLOAD_0:
		case Const.LLOAD_1:
		case Const.LLOAD_2:
		case Const.LLOAD_3:
			return push(BasicType.LONG);
		case Const.LOOKUPSWITCH:
		case Const.TABLESWITCH:
		case Const.LSHL:
		case Const.LSHR:
		case Const.LUSHR:
			return pop();
		case Const.POP:
		case Const.MONITORENTER:
		case Const.MONITOREXIT:
			return pop();
		case Const.MULTIANEWARRAY:
			return popThenPush(((MULTIANEWARRAY) ins).getDimensions(), ((MULTIANEWARRAY) ins).getType(cpg));
		case Const.NEW:
			return push(new UninitializedObjectTypeImpl(((NEW) ins).getLoadClassType(cpg), ih.getPosition()));
		case Const.NEWARRAY:
			return popThenPush(((NEWARRAY) ins).getType());
		case Const.NOP:
			return this;
		case Const.POP2:
			return pop2();
		case Const.PUTFIELD:
			return pop(1 + ((PUTFIELD) ins).getFieldType(cpg).getSize());
		case Const.PUTSTATIC:
			return pop(((PUTSTATIC) ins).getFieldType(cpg).getSize());
		case Const.SALOAD:
			return pop2ThenPush(BasicType.INT);
		case Const.SASTORE:
			return pop3();
		case Const.SWAP: {
			Type[] stack = this.stack.clone();
			stack[stack.length - 1] = this.stack[stack.length - 2];
			stack[stack.length - 2] = this.stack[stack.length - 1];
			return new TypesImpl(locals, stack);
		}
		default:
			throw new TypeInferenceException("Unknown instruction " + ins);
		}
	}

	/**
	 * Yields the types resulting from an exceptional execution in a state
	 * with these types, for the given exception class. The result will contain
	 * the same locals and only a stack element, whose type is the exception class.
	 * 
	 * @param exceptionClass the class of the exception
	 * @return the resulting types
	 */
	TypesImpl afterException(ObjectType exceptionClass) {
		return new TypesImpl(locals, new Type[] { exceptionClass });
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean same(TypesImpl previous) {
		return stack.length == 0 && Arrays.equals(locals, previous.locals);
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean sameLocalsOneStackItem(TypesImpl previous) {
		return stack.length > 0 && stack.length == stack[0].getSize() && Arrays.equals(locals, previous.locals);
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}, plus
	 * an additional local.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean append1(TypesImpl previous) {
		return stack.length == 0 && locals.length > previous.locals.length
			&& locals.length == previous.locals.length + locals[previous.locals.length].getSize()
			&& IntStream.range(0, previous.locals.length).allMatch(index -> previous.locals[index].equals(locals[index]));
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}, plus
	 * two additional locals.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean append2(TypesImpl previous) {
		return stack.length == 0 && locals.length > previous.locals.length
			&& IntStream.range(0, previous.locals.length).allMatch(index -> previous.locals[index].equals(locals[index]))
			&& getLocalsOnlyFirstSlots().skip(previous.getLocalsOnlyFirstSlots().count()).count() == 2;
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}, plus
	 * three additional locals.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean append3(TypesImpl previous) {
		return stack.length == 0 && locals.length > previous.locals.length
			&& IntStream.range(0, previous.locals.length).allMatch(index -> previous.locals[index].equals(locals[index]))
			&& getLocalsOnlyFirstSlots().skip(previous.getLocalsOnlyFirstSlots().count()).count() == 3;
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}, but
	 * for the highest local variable, that disappeared.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean chop1(TypesImpl previous) {
		return stack.length == 0 && locals.length < previous.locals.length
			&& previous.locals.length == locals.length + previous.locals[locals.length].getSize()
			&& IntStream.range(0, locals.length).allMatch(index -> previous.locals[index].equals(locals[index]));
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}, but
	 * for the highest two local variables, that disappeared.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean chop2(TypesImpl previous) {
		return stack.length == 0 && locals.length < previous.locals.length
			&& IntStream.range(0, locals.length).allMatch(index -> previous.locals[index].equals(locals[index]))
			&& previous.getLocalsOnlyFirstSlots().skip(getLocalsOnlyFirstSlots().count()).count() == 2;
	}

	/**
	 * Determines if this types have empty stack and the same locals as in {@code previous}, but
	 * for the highest three local variables, that disappeared.
	 * 
	 * @param previous the previous types
	 * @return true if and only if the above condition holds
	 */
	boolean chop3(TypesImpl previous) {
		return stack.length == 0 && locals.length < previous.locals.length
			&& IntStream.range(0, locals.length).allMatch(index -> previous.locals[index].equals(locals[index]))
			&& previous.getLocalsOnlyFirstSlots().skip(getLocalsOnlyFirstSlots().count()).count() == 3;
	}
}