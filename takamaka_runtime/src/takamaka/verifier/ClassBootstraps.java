package takamaka.verifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import takamaka.translator.Dummy;
import takamaka.translator.IncompleteClasspathError;

/**
 * An object that provides utility methods about the lambda bootstraps
 * contained in a class.
 */
public class ClassBootstraps {

	/**
	 * The class whose bootstraps are considered.
	 */
	private final VerifiedClassGen clazz;

	/**
	 * The bootstrap methods of the class.
	 */
	private final BootstrapMethod[] bootstrapMethods;

	/**
	 * The bootstrap methods of the class that lead to an entry, possibly indirectly.
	 */
	private final Set<BootstrapMethod> bootstrapMethodsLeadingToEntries = new HashSet<>();

	private final static BootstrapMethod[] NO_BOOTSTRAPS = new BootstrapMethod[0];

	ClassBootstraps(VerifiedClassGen clazz) {
		this.clazz = clazz;
		this.bootstrapMethods = computeBootstraps();
		collectBootstrapsLeadingToEntries();
	}

	/**
	 * Determines if the given bootstrap method is a method reference to an entry.
	 * 
	 * @param bootstrap the bootstrap method
	 * @return true if and only if that condition holds
	 */
	public boolean lambdaIsEntry(BootstrapMethod bootstrap) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
			ConstantPoolGen cpg = clazz.getConstantPool();

			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			if (constant instanceof ConstantMethodHandle) {
				ConstantMethodHandle mh = (ConstantMethodHandle) constant;
				Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref) {
					ConstantMethodref mr = (ConstantMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();

					return clazz.getClassLoader().isEntryPossiblyAlreadyInstrumented(className, methodName, methodSignature);
				}
			}
		};

		return false;
	}

	/**
	 * Yields the bootstrap methods in this class.
	 * 
	 * @return the bootstrap methods
	 */
	public Stream<BootstrapMethod> getBootstraps() {
		return Stream.of(bootstrapMethods);
	}

	/**
	 * Yields the subset of the bootstrap methods of this class that lead to an entry,
	 * possibly indirectly.
	 * 
	 * @return the bootstrap methods that lead to an entry
	 */
	public Stream<BootstrapMethod> getBootstrapsLeadingToEntries() {
		return bootstrapMethodsLeadingToEntries.stream();
	}

	/**
	 * Yields the bootstrap method associated with the given instruction.
	 * 
	 * @param invokedynamic the instruction
	 * @return the bootstrap method
	 */
	public BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic) {
		ConstantInvokeDynamic cid = (ConstantInvokeDynamic) clazz.getConstantPool().getConstant(invokedynamic.getIndex());
		return bootstrapMethods[cid.getBootstrapMethodAttrIndex()];
	}

	/**
	 * Yields the lambda method that is called by the given instruction.
	 * 
	 * @param invokedynamic the instruction
	 * @return the lambda method
	 */
	public Executable getTargetOf(INVOKEDYNAMIC invokedynamic) {
		return getTargetOf(getBootstrapFor(invokedynamic)).get();
	}

	/**
	 * Determines if the given instruction calls an {@code @@Entry}, possibly indirectly.
	 * 
	 * @param ih the instruction
	 * @param alsoIndirectly true if the call might also occur indirectly
	 * @return true if and only if that condition holds
	 */
	public boolean callsEntry(InstructionHandle ih, boolean alsoIndirectly) {
		Instruction instruction = ih.getInstruction();
	
		if (instruction instanceof INVOKEDYNAMIC) {
			BootstrapMethod bootstrap = getBootstrapFor((INVOKEDYNAMIC) instruction);
			if (alsoIndirectly)
				return bootstrapMethodsLeadingToEntries.contains(bootstrap);
			else
				return lambdaIsEntry(bootstrap);
		}
		else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC)) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ConstantPoolGen cpg = clazz.getConstantPool();
			ReferenceType receiver = invoke.getReferenceType(cpg);
			if (receiver instanceof ObjectType)
				return clazz.getClassLoader().isEntryPossiblyAlreadyInstrumented
					(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getSignature(cpg));
		}
	
		return false;
	}

	/**
	 * Yields the target method or constructor called by the given bootstrap. It can also be outside
	 * the class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @return the target called method or constructor
	 */
	public Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap) {
		ConstantPoolGen cpg = clazz.getConstantPool();

		Constant constant = cpg.getConstant(bootstrap.getBootstrapMethodRef());
		if (constant instanceof ConstantMethodHandle) {
			ConstantMethodHandle mh = (ConstantMethodHandle) constant;
			Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
			if (constant2 instanceof ConstantMethodref) {
				ConstantMethodref mr = (ConstantMethodref) constant2;
				int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
				String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
				ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
				String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
				String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
	
				return getTargetOfCallSite(bootstrap, className, methodName, methodSignature);
			}
		}
	
		return Optional.empty();
	}

	Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction ins) {
		if (ins instanceof INVOKEDYNAMIC)
			// invokedynamic can call a target that is an optimized reference to an executable
			return getTargetOf(getBootstrapFor((INVOKEDYNAMIC) ins));

		ConstantPoolGen cpg = clazz.getConstantPool();
		String methodName = ins.getMethodName(cpg);
		ReferenceType receiver = ins.getReferenceType(cpg);
		// it is possible to call a method on an array: in that case, the callee is a method of java.lang.Object
		String receiverClassName = receiver instanceof ObjectType ? ((ObjectType) receiver).getClassName() : "java.lang.Object";
		Class<?>[] args = clazz.getClassLoader().bcelToClass(ins.getArgumentTypes(cpg));

		if (ins instanceof INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(methodName))
			return resolveConstructorWithPossiblyExpandedArgs(receiverClassName, args);
		else {
			Class<?> returnType = clazz.getClassLoader().bcelToClass(ins.getReturnType(cpg));

			if (ins instanceof INVOKEINTERFACE)
				return resolveInterfaceMethodWithPossiblyExpandedArgs(receiverClassName, methodName, args, returnType);
			else
				return resolveMethodWithPossiblyExpandedArgs(receiverClassName, methodName, args, returnType);
		}
	}

	/**
	 * Yields the lambda bridge method called by the given bootstrap.
	 * It must belong to the same class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @return the lambda bridge method
	 */
	Optional<Method> getLambdaFor(BootstrapMethod bootstrap) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
			ConstantPoolGen cpg = clazz.getConstantPool();
			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			if (constant instanceof ConstantMethodHandle) {
				ConstantMethodHandle mh = (ConstantMethodHandle) constant;
				Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref) {
					ConstantMethodref mr = (ConstantMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
	
					// a lambda bridge can only be present in the same class that calls it
					if (className.equals(clazz.getClassName()))
						return Stream.of(clazz.getMethods())
							.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
							.findFirst();
				}
			}
		}
	
		return Optional.empty();
	}

	private Optional<Constructor<?>> resolveConstructorWithPossiblyExpandedArgs(String className, Class<?>[] args) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<Constructor<?>> result = clazz.getClassLoader().resolveConstructor(className, args);
			// we try to add the instrumentation arguments. This is important when
			// a bootstrap calls an entry of a jar already installed (and instrumented)
			// in blockchain. In that case, it will find the target only with these
			// extra arguments added during instrumentation
			return result.isPresent() ? result : clazz.getClassLoader().resolveConstructor(className, expandArgsForEntry(args));
		});
	}

	private Optional<java.lang.reflect.Method> resolveMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<java.lang.reflect.Method> result = clazz.getClassLoader().resolveMethod(className, methodName, args, returnType);
			return result.isPresent() ? result : clazz.getClassLoader().resolveMethod(className, methodName, expandArgsForEntry(args), returnType);
		});
	}

	private Optional<java.lang.reflect.Method> resolveInterfaceMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<java.lang.reflect.Method> result = clazz.getClassLoader().resolveInterfaceMethod(className, methodName, args, returnType);
			return result.isPresent() ? result : clazz.getClassLoader().resolveInterfaceMethod(className, methodName, expandArgsForEntry(args), returnType);
		});
	}

	private Class<?>[] expandArgsForEntry(Class<?>[] args) throws ClassNotFoundException {
		Class<?>[] expandedArgs = new Class<?>[args.length + 2];
		System.arraycopy(args, 0, expandedArgs, 0, args.length);
		expandedArgs[args.length] = clazz.getClassLoader().contractClass;
		expandedArgs[args.length + 1] = Dummy.class;
		return expandedArgs;
	}

	private Optional<? extends Executable> getTargetOfCallSite(BootstrapMethod bootstrap, String className, String methodName, String methodSignature) {
		ConstantPoolGen cpg = clazz.getConstantPool();

		if ("java.lang.invoke.LambdaMetafactory".equals(className) &&
				"metafactory".equals(methodName) &&
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {
	
			// this is the standard factory used to create call sites
			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			if (constant instanceof ConstantMethodHandle) {
				ConstantMethodHandle mh = (ConstantMethodHandle) constant;
				Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref) {
					ConstantMethodref mr = (ConstantMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
					Class<?>[] args = clazz.getClassLoader().bcelToClass(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = clazz.getClassLoader().bcelToClass(Type.getReturnType(methodSignature2));
	
					if (Const.CONSTRUCTOR_NAME.equals(methodName2))
						return resolveConstructorWithPossiblyExpandedArgs(className2, args);
					else
						return resolveMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
				}
				else if (constant2 instanceof ConstantInterfaceMethodref) {
					ConstantInterfaceMethodref mr = (ConstantInterfaceMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
					Class<?>[] args = clazz.getClassLoader().bcelToClass(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = clazz.getClassLoader().bcelToClass(Type.getReturnType(methodSignature2));
	
					return resolveInterfaceMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
				}
			}
		}
		else if ("java.lang.invoke.StringConcatFactory".equals(className) &&
				"makeConcatWithConstants".equals(methodName) &&
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {
	
			// this factory is used to create call sites that lead to string concatenation of every
			// possible argument type. Generically, we yield the Objects.toString(Object) method, since
			// all parameters must be checked in order for the call to be white-listed
			try {
				return Optional.of(Objects.class.getMethod("toString", Object.class));
			}
			catch (NoSuchMethodException | SecurityException e) {
				throw new IncompleteClasspathError(new ClassNotFoundException("java.util.Objects"));
			}
		}
	
		return Optional.empty();
	}

	private BootstrapMethod[] computeBootstraps() {
		Optional<BootstrapMethods> bootstraps = Stream.of(clazz.getAttributes())
			.filter(attribute -> attribute instanceof BootstrapMethods)
			.map(attribute -> (BootstrapMethods) attribute)
			.findFirst();

		return bootstraps.isPresent() ? bootstraps.get().getBootstrapMethods() : NO_BOOTSTRAPS;
	}

	private void collectBootstrapsLeadingToEntries() {
		int initialSize;
		do {
			initialSize = bootstrapMethodsLeadingToEntries.size();
			getBootstraps()
				.filter(bootstrap -> lambdaIsEntry(bootstrap) || lambdaCallsEntry(bootstrap))
				.forEach(bootstrapMethodsLeadingToEntries::add);
		}
		while (bootstrapMethodsLeadingToEntries.size() > initialSize);
	}

	private boolean lambdaCallsEntry(BootstrapMethod bootstrap) {
		Optional<Method> lambda = getLambdaFor(bootstrap);
		return lambda.isPresent() && callsEntry(lambda.get());
	}

	/**
	 * Determines if the given lambda method calls an entry, possibly indirectly.
	 * 
	 * @param lambda the lambda method
	 * @return true if that condition holds
	 */
	private boolean callsEntry(Method lambda) {
		if (lambda.getCode() != null) {
			MethodGen mg = new MethodGen(lambda, clazz.getClassName(), clazz.getConstantPool());
			return StreamSupport.stream(mg.getInstructionList().spliterator(), false)
				.anyMatch(ih -> callsEntry(ih, true));
		}

		return false;
	}
}