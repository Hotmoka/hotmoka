package io.takamaka.code.instrumentation.internal.instrumentationsOfMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.Constants;
import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;
import io.takamaka.code.verification.Bootstraps;
import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.whitelisting.MustBeFalse;
import io.takamaka.code.whitelisting.MustRedefineHashCodeOrToString;
import io.takamaka.code.whitelisting.WhiteListingProofObligation;

/**
 * Adds instructions that check that white-listing proof obligations hold at run time.
 */
public class AddRuntimeChecksForWhiteListingProofObligations extends InstrumentedClassImpl.Builder.MethodLevelInstrumentation {
	private final static short PRIVATE_SYNTHETIC_STATIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_STATIC;

	public AddRuntimeChecksForWhiteListingProofObligations(InstrumentedClassImpl.Builder builder, MethodGen method) {
		builder.super(method);

		if (!method.isAbstract())
			for (InstructionHandle ih: method.getInstructionList()) {
				Instruction ins = ih.getInstruction();
				if (ins instanceof FieldInstruction) {
					FieldInstruction fi = (FieldInstruction) ins;
					Field model = verifiedClass.whiteListingModelOf(fi);
					if (hasProofObligations(model))
						// proof obligations are currently not implemented nor used on fields
						throw new IllegalStateException("unexpected white-listing proof obligation for field " + fi.getReferenceType(cpg) + "." + fi.getFieldName(cpg));
				}
				else if (ins instanceof InvokeInstruction) {
					// we share the same checker for equivalent invoke instructions
					String key = keyFor(ih);
					InvokeInstruction replacement = whiteListingCache.get(key);
					if (replacement != null)
						ih.setInstruction(replacement);
					else {
						Executable model = verifiedClass.whiteListingModelOf((InvokeInstruction) ins);
						if (hasProofObligations(model)) {
							replacement = addWhiteListVerificationMethod(ih, (InvokeInstruction) ins, model, key);
							whiteListingCache.put(key, replacement);
							ih.setInstruction(replacement);
						}
					}
				}
			}
	}

	/**
	 * Adds a static method to the class under instrumentation, that checks that the
	 * white-listing proof obligations in the model hold at run time.
	 * 
	 * @param ins the call instruction whose parameters must be verified
	 * @param model the model that contains the proof obligations in order, for the
	 *              call, to be white-listed
	 * @param key the key used to identify equivalent invoke instructions. It is used to check which
	 *            proof obligations need to be checked at run time
	 * @return the invoke instruction that must be used, instead of {@code ins}, to
	 *         call the freshly added method
	 */
	private InvokeInstruction addWhiteListVerificationMethod(InstructionHandle ih, InvokeInstruction ins, Executable model, String key) {
		if (ins instanceof INVOKEDYNAMIC)
			if (isCallToConcatenationMetaFactory((INVOKEDYNAMIC) ins))
				return addWhiteListVerificationMethodForINVOKEDYNAMICForStringConcatenation((INVOKEDYNAMIC) ins);
			else
				return addWhiteListVerificationMethod((INVOKEDYNAMIC) ins, model);
		else
			return addWhiteListVerificationMethodForNonINVOKEDYNAMIC(ih, ins, model, key);
	}

	private String keyFor(InstructionHandle ih) {
		InvokeInstruction ins = (InvokeInstruction) ih.getInstruction();

		String key;
		if (ins instanceof INVOKEDYNAMIC)
			key = ins.getName() + " #" + ((ConstantInvokeDynamic) cpg.getConstant(((INVOKEDYNAMIC) ins).getIndex())).getBootstrapMethodAttrIndex();
		else {
			key = ins.getName() + " " + ins.getReferenceType(cpg) + "." + ins.getMethodName(cpg) + ins.getSignature(cpg);
			// we add a mask that specifies the white-listing proof obligations that can be discharged, since
			// we can use the same verifier only if two instructions need verification of the same proof obligations
			Executable model = verifiedClass.whiteListingModelOf(ins);
			if (hasProofObligations(model)) {
				int slots = ins.consumeStack(cpg);
				String mask = "";

				if (!(ins instanceof INVOKESTATIC)) {
					int slotsCopy = slots;
					mask += Stream.of(model.getAnnotations())
							.map(Annotation::annotationType)
							.filter(annotationType -> annotationType.isAnnotationPresent(WhiteListingProofObligation.class))
							.map(annotationType -> canBeStaticallyDicharged(annotationType, ih, slotsCopy) ? "0" : "1")
							.collect(Collectors.joining());
					slots--;
				}

				Annotation[][] anns = model.getParameterAnnotations();
				int par = 0;
				for (Type argType: ins.getArgumentTypes(cpg)) {
					int slotsCopy = slots;
					mask += Stream.of(anns[par])
							.flatMap(Stream::of)
							.map(Annotation::annotationType)
							.filter(annotationType -> annotationType.isAnnotationPresent(WhiteListingProofObligation.class))
							.map(annotationType -> canBeStaticallyDicharged(annotationType, ih, slotsCopy) ? "0" : "1")
							.collect(Collectors.joining());
					par++;
					slots -= argType.getSize();
				}

				key = mask + ": " + key;
			}
		}

		return key;
	}

	private InvokeInstruction addWhiteListVerificationMethodForINVOKEDYNAMICForStringConcatenation(INVOKEDYNAMIC invokedynamic) {
		String verifierName = getNewNameForPrivateMethod(Constants.EXTRA_VERIFIER_NAME);
		InstructionList il = new InstructionList();
		String signature = invokedynamic.getSignature(cpg);
		Type verifierReturnType = Type.getReturnType(signature);
		Type[] args = Type.getArgumentTypes(signature);

		int index = 0;
		boolean atLeastOneCheck = false;

		for (Type argType: args) {
			il.append(InstructionFactory.createLoad(argType, index));
			index += argType.getSize();
			if (argType instanceof ObjectType) {
				Class<?> argClass = ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> classLoader.loadClass(((ObjectType) argType).getClassName()));

				// we check if we can statically verify that the value redefines hashCode or toString
				if (!redefinesHashCodeOrToString(argClass)) {
					il.append(InstructionFactory.createDup(argType.getSize()));
					il.append(factory.createConstant("string concatenation"));
					il.append(createInvokeForWhiteListingCheck(getWhiteListingCheckFor(MustRedefineHashCodeOrToString.class).get()));
					atLeastOneCheck = true;
				}
			}
		}

		// if all proof obligations can be discharged statically, we do not generate
		// any verification method and yield the same invoke instruction. Note that this
		// optimization depends on the static types of the arguments of the call only,
		// hence it can be safely cached
		if (!atLeastOneCheck)
			return invokedynamic;

		il.append(invokedynamic);
		il.append(InstructionFactory.createReturn(verifierReturnType));

		MethodGen addedVerifier = new MethodGen(PRIVATE_SYNTHETIC_STATIC, verifierReturnType, args, null, verifierName, className, il, cpg);
		addMethod(addedVerifier, false);

		return factory.createInvoke(className, verifierName, verifierReturnType, args, Const.INVOKESTATIC);
	}

	private boolean redefinesHashCodeOrToString(Class<?> clazz) {
		return Stream.of(clazz.getMethods())
			.filter(method -> !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() != Object.class)
			.map(java.lang.reflect.Method::getName)
			.anyMatch(name -> "hashCode".equals(name) || "toString".equals(name));
	}

	/**
	 * Adds a static method to the class under instrumentation, that checks that the
	 * white-listing proof obligations in the model hold at run time.
	 * 
	 * @param invokedynamic the call instruction whose parameters must be verified
	 * @param model         the model that contains the proof obligations in order,
	 *                      for the call, to be white-listed
	 */
	private InvokeInstruction addWhiteListVerificationMethod(INVOKEDYNAMIC invokedynamic, Executable model) {
		String verifierName = getNewNameForPrivateMethod(Constants.EXTRA_VERIFIER_NAME);
		Bootstraps classBootstraps = verifiedClass.getBootstraps();
		InstructionList il = new InstructionList();
		List<Type> args = new ArrayList<>();
		BootstrapMethod bootstrap = classBootstraps.getBootstrapFor(invokedynamic);
		int[] bootstrapArgs = bootstrap.getBootstrapArguments();
		ConstantMethodHandle mh = (ConstantMethodHandle) cpg.getConstant(bootstrapArgs[1]);
		int invokeKind = mh.getReferenceKind();
		Executable target = classBootstraps.getTargetOf(bootstrap).get();
		Class<?> receiverClass = target.getDeclaringClass();
		if (receiverClass.isArray())
			receiverClass = Object.class;
		Type receiver = Type.getType(receiverClass);
		Type verifierReturnType = target instanceof Constructor<?> ? Type.VOID : Type.getType(((java.lang.reflect.Method) target).getReturnType());
		int index = 0;

		if (!Modifier.isStatic(target.getModifiers())) {
			il.append(InstructionFactory.createLoad(receiver, index));
			index += receiver.getSize();
			addWhiteListingChecksFor(null, model.getAnnotations(), receiver, il, target.getName(), null, -1);
		}

		int par = 0;
		Annotation[][] anns = model.getParameterAnnotations();

		for (Class<?> arg : target.getParameterTypes()) {
			Type argType = Type.getType(arg);
			args.add(argType);
			il.append(InstructionFactory.createLoad(argType, index));
			index += argType.getSize();
			addWhiteListingChecksFor(null, anns[par], argType, il, target.getName(), null, -1);
			par++;
		}

		Type[] argsAsArray = args.toArray(new Type[args.size()]);
		il.append(factory.createInvoke(receiverClass.getName(), target.getName(), verifierReturnType, argsAsArray,
				invokeCorrespondingToBootstrapInvocationType(invokeKind)));
		il.append(InstructionFactory.createReturn(verifierReturnType));

		MethodGen addedVerifier = new MethodGen(PRIVATE_SYNTHETIC_STATIC, verifierReturnType, argsAsArray, null, verifierName, className, il, cpg);
		addMethod(addedVerifier, false);

		// replace inside the bootstrap method
		bootstrapArgs[1] = addMethodHandleToConstantPool(new ConstantMethodHandle(Const.REF_invokeStatic, cpg
			.addMethodref(className, verifierName, Type.getMethodSignature(verifierReturnType, argsAsArray))));

		// we return the same invoke instruction, but its bootstrap method has been modified
		return invokedynamic;
	}

	private boolean hasProofObligations(Field field) {
		return Stream.of(field.getAnnotations()).map(Annotation::annotationType).anyMatch(annotation -> annotation.isAnnotationPresent(WhiteListingProofObligation.class));
	}

	private boolean hasProofObligations(Executable method) {
		return Stream.of(method.getAnnotations()).map(Annotation::annotationType).anyMatch(annotation -> annotation.isAnnotationPresent(WhiteListingProofObligation.class))
				||
			Stream.of(method.getParameterAnnotations()).flatMap(Stream::of).map(Annotation::annotationType).anyMatch(annotation -> annotation.isAnnotationPresent(WhiteListingProofObligation.class));
	}

	private boolean canBeStaticallyDicharged(Class<? extends Annotation> annotationType, InstructionHandle ih, int slots) {
		// ih contains an InvokeInstruction distinct from INVOKEDYNAMIC
		List<Instruction> pushers = new ArrayList<>();

		if (annotationType == MustBeFalse.class) {
			forEachPusher(ih, slots, where -> pushers.add(where.getInstruction()), () -> pushers.add(null));
			return pushers.stream().allMatch(ins -> ins != null && ins instanceof ICONST && ((ICONST) ins).getValue().equals(0));
		}

		return false;
	}

	private boolean isCallToConcatenationMetaFactory(INVOKEDYNAMIC invokedynamic) {
		BootstrapMethod bootstrap = verifiedClass.getBootstraps().getBootstrapFor(invokedynamic);
		Constant constant = cpg.getConstant(bootstrap.getBootstrapMethodRef());
		ConstantMethodHandle mh = (ConstantMethodHandle) constant;
		Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
		ConstantMethodref mr = (ConstantMethodref) constant2;
		int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
		String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
		ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
		String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
		String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();

		// this meta-factory is used by Java compilers for optimized concatenation into string
		return "java.lang.invoke.StringConcatFactory".equals(className)
			&& "makeConcatWithConstants".equals(methodName)
			&& "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;".equals(methodSignature);
	}

	/**
	 * Adds a static method to the class under instrumentation, that checks that the
	 * white-listing proof obligations in the model hold at run time.
	 * 
	 * @param invoke the call instruction whose parameters must be verified
	 * @param key the key used to identify equivalent invoke instructions. It is used to check which
	 *            proof obligations need to be checked at run time
	 * @param model the model that contains the proof obligations in order, for the
	 *              call, to be white-listed
	 */
	private InvokeInstruction addWhiteListVerificationMethodForNonINVOKEDYNAMIC(InstructionHandle ih, InvokeInstruction invoke, Executable model, String key) {
		String verifierName = getNewNameForPrivateMethod(Constants.EXTRA_VERIFIER_NAME);
		Type verifierReturnType = invoke.getReturnType(cpg);
		String methodName = invoke.getMethodName(cpg);
		InstructionList il = new InstructionList();
		List<Type> args = new ArrayList<>();
		int index = 0;
		boolean atLeastOne = false;
		int annotationsCursor = 0;

		if (!(invoke instanceof INVOKESTATIC)) {
			ReferenceType receiver;

			if (invoke instanceof INVOKESPECIAL && !Const.CONSTRUCTOR_NAME.equals(methodName)) {
				// call to a private instance method or to an instance method through super.m():
				// we provide a more precise type for the receiver, that is needed for JVM verification
				receiver = new ObjectType(className);
				args.add(receiver);
			}
			else {
				receiver = invoke.getReferenceType(cpg);
				if (receiver instanceof ObjectType)
					args.add(receiver);
				else
					args.add(ObjectType.OBJECT);
			}

			il.append(InstructionFactory.createLoad(receiver, index));
			Annotation[] anns = model.getAnnotations();
			atLeastOne |= addWhiteListingChecksFor(ih, anns, receiver, il, methodName, key, annotationsCursor);
			index++;
			annotationsCursor += anns.length;
		}

		int par = 0;
		Annotation[][] anns = model.getParameterAnnotations();

		for (Type argType: invoke.getArgumentTypes(cpg)) {
			args.add(argType);
			il.append(InstructionFactory.createLoad(argType, index));
			atLeastOne |= addWhiteListingChecksFor(ih, anns[par], argType, il, methodName, key, annotationsCursor);
			index += argType.getSize();
			annotationsCursor += anns[par].length;
			par++;
		}

		if (!atLeastOne)
			// all proof obligations can be discharged statically: we do not generate the checker
			return invoke;

		il.append(invoke);
		il.append(InstructionFactory.createReturn(verifierReturnType));

		Type[] argsAsArray = args.toArray(new Type[args.size()]);
		MethodGen addedVerifier = new MethodGen(PRIVATE_SYNTHETIC_STATIC, verifierReturnType, argsAsArray, null, verifierName, className, il, cpg);
		addMethod(addedVerifier, false);

		return factory.createInvoke(className, verifierName, verifierReturnType, argsAsArray, Const.INVOKESTATIC);
	}

	private boolean addWhiteListingChecksFor(InstructionHandle ih, Annotation[] annotations, Type argType, InstructionList il, String methodName, String key, int annotationsCursor) {
		int initialSize = il.getLength();

		for (Annotation ann: annotations) {
			Optional<java.lang.reflect.Method> checkMethod = getWhiteListingCheckFor(ann);
			if (checkMethod.isPresent())
				// we check if the annotation could not be statically discharged
				if (ih == null || key.charAt(annotationsCursor++) == '1') {
					il.append(InstructionFactory.createDup(argType.getSize()));
					il.append(factory.createConstant(methodName));
					il.append(createInvokeForWhiteListingCheck(checkMethod.get()));
				}
		}

		return il.getLength() > initialSize;
	}

	private Optional<java.lang.reflect.Method> getWhiteListingCheckFor(Annotation annotation) {
		return getWhiteListingCheckFor(annotation.annotationType());
	}

	private Optional<java.lang.reflect.Method> getWhiteListingCheckFor(Class<? extends Annotation> annotationType) {
		if (annotationType.isAnnotationPresent(WhiteListingProofObligation.class)) {
			String checkName = lowerInitial(annotationType.getSimpleName());
			Optional<java.lang.reflect.Method> checkMethod = Stream.of(getClass().getDeclaredMethods())
				.filter(method -> method.getName().equals(checkName)).findFirst();

			if (!checkMethod.isPresent())
				throw new IllegalStateException("unexpected white-list annotation " + annotationType.getSimpleName());

			return checkMethod;
		}

		return Optional.empty();
	}

	private String lowerInitial(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

	private InvokeInstruction createInvokeForWhiteListingCheck(java.lang.reflect.Method checkMethod) {
		return factory.createInvoke(Constants.ABSTRACT_TAKAMAKA_NAME, checkMethod.getName(), Type.VOID,
			new Type[] { Type.getType(checkMethod.getParameterTypes()[0]), Type.STRING }, Const.INVOKESTATIC);
	}

	@SuppressWarnings("unused")
	private void mustBeFalse(boolean value, String methodName) {}

	@SuppressWarnings("unused")
	private void mustRedefineHashCode(Object value, String methodName) {}

	@SuppressWarnings("unused")
	private void mustRedefineHashCodeOrToString(Object value, String methodName) {}
}