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

package io.hotmoka.node.internal.signatures;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.json.MethodSignatureJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The signature of a method of a class.
 */
@Immutable
public abstract class AbstractMethodSignature extends AbstractCodeSignature implements MethodSignature {

	/**
	 * The name of the method.
	 */
	private final String name;

	/**
	 * Builds the signature of a method.
	 * 
	 * @param <E> the type of the exception thrown if some arguments is illegal
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	protected <E extends Exception> AbstractMethodSignature(ClassType definingClass, String name, StorageType[] formals, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(definingClass, formals, onIllegalArgs);

		this.name = Objects.requireNonNull(name, "name cannot be null", onIllegalArgs);
	}

	/**
	 * Yields a method signature from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the method signature
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public static MethodSignature from(MethodSignatureJson json) throws InconsistentJsonException {
		return json.getReturnType().isPresent() ? new NonVoidMethodSignatureImpl(json) : new VoidMethodSignatureImpl(json);
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getDefiningClass() + "." + name + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof MethodSignature ms && name.equals(ms.getName()) && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ name.hashCode();
	}

	/**
	 * Factory method that unmarshals a method signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the method signature
	 * @throws IOException if the method signature cannot be unmarshalled
	 */
	public static MethodSignature from(UnmarshallingContext context) throws IOException {
		// TODO: introduce optimized representation of methods
		int length = context.readCompactInt();

		// we determine if the method is void or not, by looking at the parity of the number of formals
		// (see the into() method in NonVoidMethodSignatureImpl and VoidMethodSignatureImpl)
		if (length % 2 == 0)
			return new VoidMethodSignatureImpl(context, length / 2);
		else
			return new NonVoidMethodSignatureImpl(context, length / 2);
	}

	protected static StorageType[] unmarshalFormals(UnmarshallingContext context, int numberOfFormals) throws IOException {
		var formals = new StorageType[numberOfFormals];
		for (int pos = 0; pos < numberOfFormals; pos++)
			formals[pos] = StorageTypes.from(context);

		return formals;
	}
}