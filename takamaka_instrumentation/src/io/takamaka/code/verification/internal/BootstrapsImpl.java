package io.takamaka.code.verification.internal;

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
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.Bootstraps;
import io.takamaka.code.verification.IncompleteClasspathError;
import io.takamaka.code.verification.VerifiedClass;

/**
 * An object that provides utility methods about the lambda bootstraps
 * contained in a class.
 */
public class BootstrapsImpl implements Bootstraps {

	/**
	 * The class whose bootstraps are considered.
	 */
	private final VerifiedClass clazz;

	/**
	 * The constant pool of the class whose bootstraps are considered.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * The bootstrap methods of the class.
	 */
	private final BootstrapMethod[] bootstrapMethods;

	/**
	 * The bootstrap methods of the class that lead to an entry, possibly indirectly.
	 */
	private final Set<BootstrapMethod> bootstrapMethodsLeadingToEntries = new HashSet<>();

	private final static BootstrapMethod[] NO_BOOTSTRAPS = new BootstrapMethod[0];

	public BootstrapsImpl(VerifiedClass clazz) {
		this.clazz = clazz;
		this.cpg = clazz.getConstantPool();
		this.bootstrapMethods = computeBootstraps();
		collectBootstrapsLeadingToEntries();
	}

	@Override
	public boolean lambdaIsEntry(BootstrapMethod bootstrap) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
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

					return clazz.jar.getAnnotations().isEntryPossiblyAlreadyInstrumented(className, methodName, methodSignature);
				}
			}
		};

		return false;
	}

	@Override
	public Stream<BootstrapMethod> getBootstraps() {
		return Stream.of(bootstrapMethods);
	}

	@Override
	public Stream<BootstrapMethod> getBootstrapsLeadingToEntries() {
		return bootstrapMethodsLeadingToEntries.stream();
	}

	@Override
	public BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic) {
		ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());
		return bootstrapMethods[cid.getBootstrapMethodAttrIndex()];
	}

	@Override
	public Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap) {
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

	@Override
	public Optional<MethodGen> getLambdaFor(BootstrapMethod bootstrap) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
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
						return clazz.getMethodGens()
							.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
							.findFirst();
				}
			}
		}
	
		return Optional.empty();
	}

	private Optional<? extends Executable> getTargetOfCallSite(BootstrapMethod bootstrap, String className, String methodName, String methodSignature) {
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
					Class<?>[] args = clazz.jar.getBcelToClass().of(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = clazz.jar.getBcelToClass().of(Type.getReturnType(methodSignature2));
	
					if (Const.CONSTRUCTOR_NAME.equals(methodName2))
						//TODO: remove cast
						return ((ResolverImpl) clazz.resolver).resolveConstructorWithPossiblyExpandedArgs(className2, args);
					else
						//TODO: remove cast
						return ((ResolverImpl) clazz.resolver).resolveMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
				}
				else if (constant2 instanceof ConstantInterfaceMethodref) {
					ConstantInterfaceMethodref mr = (ConstantInterfaceMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
					Class<?>[] args = clazz.jar.getBcelToClass().of(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = clazz.jar.getBcelToClass().of(Type.getReturnType(methodSignature2));

					//TODO: remove cast
					return ((ResolverImpl) clazz.resolver).resolveInterfaceMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
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

	/**
	 * Determines if the given lambda method calls an {@code @@Entry}, possibly indirectly.
	 * 
	 * @param lambda the lambda method
	 * @return true if that condition holds
	 */
	private boolean lambdaCallsEntry(BootstrapMethod bootstrap) {
		Optional<MethodGen> lambda = getLambdaFor(bootstrap);
		if (lambda.isPresent()) {
			InstructionList instructions = lambda.get().getInstructionList();
			if (instructions != null)
				return StreamSupport.stream(instructions.spliterator(), false).anyMatch(this::leadsToEntry);
		}

		return false;
	}

	/**
	 * Determines if the given instruction calls an {@code @@Entry}, possibly indirectly.
	 * 
	 * @param ih the instruction
	 * @return true if that condition holds
	 */
	private boolean leadsToEntry(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();
	
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstrapMethodsLeadingToEntries.contains(getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC)) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			return receiver instanceof ObjectType &&
				clazz.jar.getAnnotations().isEntryPossiblyAlreadyInstrumented
					(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getSignature(cpg));
		}
		else
			return false;
	}
}