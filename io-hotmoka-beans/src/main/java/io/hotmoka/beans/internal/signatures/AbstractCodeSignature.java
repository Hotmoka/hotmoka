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

package io.hotmoka.beans.internal.signatures;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.CodeSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * The signature of a method or constructor.
 */
@Immutable
public abstract class AbstractCodeSignature extends AbstractMarshallable implements CodeSignature {

	/**
	 * The class of the method or constructor.
	 */
	private final ClassType definingClass;

	/**
	 * The formal arguments of the method or constructor.
	 */
	private final StorageType[] formals;
	
	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	protected AbstractCodeSignature(ClassType definingClass, StorageType... formals) {
		this.definingClass = Objects.requireNonNull(definingClass, "definingClass cannot be null");
		this.formals = Objects.requireNonNull(formals, "formals cannot be null");
		Stream.of(formals).forEach(formal -> Objects.requireNonNull(formal, "formals cannot hold null"));
	}

	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the name of the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	public AbstractCodeSignature(String definingClass, StorageType... formals) {
		this(StorageTypes.classNamed(definingClass), formals);
	}

	@Override
	public final ClassType getDefiningClass() {
		return definingClass;
	}

	/**
	 * Yields the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the formal arguments
	 */
	public final Stream<StorageType> formals() {
		return Stream.of(formals);
	}

	/**
	 * Yields a comma-separated string of the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the string
	 */
	protected final String commaSeparatedFormals() {
		return formals()
			.map(StorageType::toString)
			.collect(Collectors.joining(",", "(", ")"));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AbstractCodeSignature cs && cs.definingClass.equals(definingClass) && Arrays.equals(cs.formals, formals);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ Arrays.hashCode(formals);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		definingClass.into(context);
		context.writeLengthAndArray(formals);
	}

	/**
	 * Factory method that unmarshals a code signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the code signature
	 * @throws IOException if the code signature cannot be unmarshalled
	 */
	public static CodeSignature from(UnmarshallingContext context) throws IOException {
		var selector = context.readByte();
		if (selector == ConstructorSignatureImpl.SELECTOR_EOA)
			return ConstructorSignatures.EOA_CONSTRUCTOR;
		else if (selector == VoidMethodSignature.SELECTOR_REWARD)
			return VoidMethodSignature.VALIDATORS_REWARD;

		ClassType definingClass;

		try {
			definingClass = (ClassType) StorageTypes.from(context);
		}
		catch (ClassCastException e) {
			throw new IOException("Failed to unmarshal a code signature", e);
		}

		var formals = context.readLengthAndArray(StorageTypes::from, StorageType[]::new);

		switch (selector) {
		case ConstructorSignatureImpl.SELECTOR: return ConstructorSignatures.of(definingClass, formals);
		case VoidMethodSignature.SELECTOR: return new VoidMethodSignature(definingClass, context.readStringUnshared(), formals);
		case NonVoidMethodSignature.SELECTOR: return new NonVoidMethodSignature(definingClass, context.readStringUnshared(), StorageTypes.from(context), formals);
		default: throw new IOException("Unexpected code signature selector: " + selector);
		}
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}