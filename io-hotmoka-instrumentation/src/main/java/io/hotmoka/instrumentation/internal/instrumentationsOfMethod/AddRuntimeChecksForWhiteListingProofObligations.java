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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
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
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.hotmoka.constants.Constants;
import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.hotmoka.whitelisting.HasDeterministicTerminatingToString;
import io.hotmoka.whitelisting.MustBeFalse;
import io.hotmoka.whitelisting.ResolvingClassLoaders;
import io.hotmoka.whitelisting.api.WhiteListingProofObligation;

/**
 * Adds instructions that check that white-listing proof obligations hold at run time.
 */
public class AddRuntimeChecksForWhiteListingProofObligations extends MethodLevelInstrumentation {
	private final static short PRIVATE_SYNTHETIC_STATIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_STATIC;
	private final static Type[] CHECK_WHITE_LISTING_PREDICATE_ARGS = new Type[] { Type.OBJECT, Type.CLASS, Type.STRING };

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 * @param method the method being instrumented
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	public AddRuntimeChecksForWhiteListingProofObligations(InstrumentedClassImpl.Builder builder, MethodGen method) throws ClassNotFoundException {
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
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka program
	 */
	private InvokeInstruction addWhiteListVerificationMethod(InstructionHandle ih, InvokeInstruction ins, Executable model, String key) throws ClassNotFoundException {
		if (ins instanceof INVOKEDYNAMIC)
			if (isCallToConcatenationMetaFactory((INVOKEDYNAMIC) ins))
				return addWhiteListVerificationMethodForINVOKEDYNAMICForStringConcatenation((INVOKEDYNAMIC) ins);
			else
				return addWhiteListVerificationMethod((INVOKEDYNAMIC) ins, model);
		else
			return addWhiteListVerificationMethodForNonINVOKEDYNAMIC(ih, ins, model, key);
	}

	private String keyFor(InstructionHandle ih) throws ClassNotFoundException {
		InvokeInstruction ins = (InvokeInstruction) ih.getInstruction();

		String key;
		if (ins instanceof INVOKEDYNAMIC)
			key = ins.getName() + " #" + ((ConstantInvokeDynamic) cpg.getConstant(ins.getIndex())).getBootstrapMethodAttrIndex();
		else {
			key = ins.getName() + " " + ins.getReferenceType(cpg) + "." + ins.getMethodName(cpg) + ins.getSignature(cpg);
			// we add a mask that specifies the white-listing proof obligations that can be discharged, since
			// we can use the same verifier only if two instructions need verification of the same proof obligations
			Executable model = verifiedClass.whiteListingModelOf(ins);
			if (hasProofObligations(model)) {
				int slots = ins.consumeStack(cpg);
				StringBuilder mask = new StringBuilder();

				if (!(ins instanceof INVOKESTATIC)) {
					int slotsCopy = slots;
					mask.append(Stream.of(model.getAnnotations())
							.map(Annotation::annotationType)
							.filter(annotationType -> annotationType.isAnnotationPresent(WhiteListingProofObligation.class))
							.map(annotationType -> canBeStaticallyDicharged(annotationType, ih, slotsCopy) ? "0" : "1")
							.collect(Collectors.joining()));
					slots--;
				}

				Annotation[][] anns = model.getParameterAnnotations();
				int par = 0;
				for (Type argType: ins.getArgumentTypes(cpg)) {
					int slotsCopy = slots;
					mask.append(Stream.of(anns[par])
							.map(Annotation::annotationType)
							.filter(annotationType -> annotationType.isAnnotationPresent(WhiteListingProofObligation.class))
							.map(annotationType -> canBeStaticallyDicharged(annotationType, ih, slotsCopy) ? "0" : "1")
							.collect(Collectors.joining()));
					par++;
					slots -= argType.getSize();
				}

				key = mask + ": " + key;
			}
		}

		return key;
	}

	private InvokeInstruction addWhiteListVerificationMethodForINVOKEDYNAMICForStringConcatenation(INVOKEDYNAMIC invokedynamic) throws ClassNotFoundException {
		String verifierName = getNewNameForPrivateMethod(InstrumentationConstants.EXTRA_VERIFIER);
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
				Class<?> argClass = classLoader.loadClass(((ObjectType) argType).getClassName());

				// we check if we can statically verify that the value redefines hashCode or toString
				if (!redefinesHashCodeOrToString(argClass)) {
					il.append(InstructionFactory.createDup(argType.getSize()));
					il.append(new LDC(cpg.addClass(HasDeterministicTerminatingToString.class.getAnnotation(WhiteListingProofObligation.class).check().getName())));
					il.append(factory.createConstant("string concatenation"));
					il.append(factory.createInvoke(Constants.RUNTIME_NAME, "checkWhiteListingPredicate", Type.VOID, CHECK_WHITE_LISTING_PREDICATE_ARGS, Const.INVOKESTATIC));
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
	 * @throws ClassNotFoundException 
	 */
	private InvokeInstruction addWhiteListVerificationMethod(INVOKEDYNAMIC invokedynamic, Executable model) throws ClassNotFoundException {
		String verifierName = getNewNameForPrivateMethod(InstrumentationConstants.EXTRA_VERIFIER);
		InstructionList il = new InstructionList();
		List<Type> args = new ArrayList<>();
		List<Type> argsWithoutReceiver = new ArrayList<>();
		BootstrapMethod bootstrap = bootstraps.getBootstrapFor(invokedynamic);
		int[] bootstrapArgs = bootstrap.getBootstrapArguments();
		ConstantMethodHandle mh = (ConstantMethodHandle) cpg.getConstant(bootstrapArgs[1]);
		ConstantUtf8 descriptor = (ConstantUtf8) cpg.getConstant(((ConstantMethodType) cpg.getConstant(bootstrapArgs[2])).getDescriptorIndex());
		int invokeKind = mh.getReferenceKind();
		Executable target = bootstraps.getTargetOf(bootstrap).get();

		ObjectType receiver;
		if (invokeKind == Const.REF_invokeStatic)
			receiver = (ObjectType) Type.getType(target.getDeclaringClass());
		else
			receiver = (ObjectType) Type.getArgumentTypes(descriptor.getBytes())[0];

		Type verifierReturnType = target instanceof Constructor<?> ? Type.VOID : Type.getType(((java.lang.reflect.Method) target).getReturnType());
		int index = 0;

		if (!Modifier.isStatic(target.getModifiers())) {
			il.append(InstructionFactory.createLoad(receiver, index));
			index += receiver.getSize();
			args.add(receiver);
			addWhiteListingChecksFor(null, model.getAnnotations(), receiver, il, target.getName(), null, -1);
		}

		int par = 0;
		Annotation[][] anns = model.getParameterAnnotations();

		for (Class<?> arg: target.getParameterTypes()) {
			Type argType = Type.getType(arg);
			args.add(argType);
			argsWithoutReceiver.add(argType);
			il.append(InstructionFactory.createLoad(argType, index));
			index += argType.getSize();
			addWhiteListingChecksFor(null, anns[par], argType, il, target.getName(), null, -1);
			par++;
		}

		Type[] argsWithoutReceiverAsArray = argsWithoutReceiver.toArray(Type[]::new);

		il.append(factory.createInvoke(receiver.getClassName(), target.getName(), verifierReturnType, argsWithoutReceiverAsArray,
				invokeCorrespondingToBootstrapInvocationType(invokeKind)));
		il.append(InstructionFactory.createReturn(verifierReturnType));

		Type[] argsAsArray = args.toArray(Type[]::new);

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
		return annotationType == MustBeFalse.class &&
			pushers.getPushers(ih, slots, method.getInstructionList(), cpg)
				.map(InstructionHandle::getInstruction)
				.allMatch(ins -> ins instanceof ICONST && ((ICONST) ins).getValue().equals(0));
	}

	private boolean isCallToConcatenationMetaFactory(INVOKEDYNAMIC invokedynamic) {
		BootstrapMethod bootstrap = bootstraps.getBootstrapFor(invokedynamic);
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
		String verifierName = getNewNameForPrivateMethod(InstrumentationConstants.EXTRA_VERIFIER);
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
			atLeastOne = addWhiteListingChecksFor(ih, anns, receiver, il, methodName, key, annotationsCursor);
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

		Type[] argsAsArray = args.toArray(Type[]::new);
		MethodGen addedVerifier = new MethodGen(PRIVATE_SYNTHETIC_STATIC, verifierReturnType, argsAsArray, null, verifierName, className, il, cpg);
		addMethod(addedVerifier, false);

		return factory.createInvoke(className, verifierName, verifierReturnType, argsAsArray, Const.INVOKESTATIC);
	}

	private boolean addWhiteListingChecksFor(InstructionHandle ih, Annotation[] annotations, Type argType, InstructionList il, String methodName, String key, int annotationsCursor) {
		int initialSize = il.getLength();

		for (Annotation ann: annotations) {
			Class<? extends Annotation> annotationType = ann.annotationType();

			// we only accept white-listing annotations from the white-listing module;
			// this avoids the risk of users sending their white-listing annotations along
			// with their code and implementing the check method in dangerous ways
			if (annotationType.getPackage() == ResolvingClassLoaders.class.getPackage()) {
				WhiteListingProofObligation wlpo = annotationType.getAnnotation(WhiteListingProofObligation.class);
				if (wlpo != null)
					// we check if the annotation could not be statically discharged
					if (ih == null || key.charAt(annotationsCursor++) == '1') {
						il.append(InstructionFactory.createDup(argType.getSize()));
						boxIfNeeded(il, argType);
						il.append(new LDC(cpg.addClass(wlpo.check().getName())));
						il.append(factory.createConstant(methodName));
						il.append(factory.createInvoke(Constants.RUNTIME_NAME, "checkWhiteListingPredicate", Type.VOID, CHECK_WHITE_LISTING_PREDICATE_ARGS, Const.INVOKESTATIC));
					}
			}
		}

		return il.getLength() > initialSize;
	}

	/**
	 * Creates code that boxes the given value, if primitive.
	 * 
	 * @param il the instruction list where boxing instructions can be added
	 * @param type the type of the value
	 */
	private void boxIfNeeded(InstructionList il, Type type) {
		String wrapperName;

		if (type == Type.INT)
			wrapperName = "java.lang.Integer";
		else if (type == Type.BOOLEAN)
			wrapperName = "java.lang.Boolean";
		else if (type == Type.CHAR)
			wrapperName = "java.lang.Character";
		else if (type == Type.SHORT)
			wrapperName = "java.lang.Short";
		else if (type == Type.LONG)
			wrapperName = "java.lang.Long";
		else if (type == Type.FLOAT)
			wrapperName = "java.lang.Float";
		else if (type == Type.DOUBLE)
			wrapperName = "java.lang.Double";
		else if (type == Type.BYTE)
			wrapperName = "java.lang.Byte";
		else
			return;

		il.append(factory.createInvoke(wrapperName, "valueOf", new ObjectType(wrapperName), new Type[] { type }, Const.INVOKESTATIC));
	}
}