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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.CodeSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.gson.MethodSignatureJson;
import io.hotmoka.node.internal.types.AbstractStorageType;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

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
	 * @param <E> the type of the exception thrown if some arguments is illegal
	 * @param definingClass the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	protected <E extends Exception> AbstractCodeSignature(ClassType definingClass, StorageType[] formals, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.definingClass = Objects.requireNonNull(definingClass, "definingClass cannot be null", onIllegalArgs);
		this.formals = Objects.requireNonNull(formals, "formals cannot be null", onIllegalArgs);
		for (var formal: formals)
			Objects.requireNonNull(formal, "formals cannot hold null", onIllegalArgs);
	}

	protected static ClassType unmarshalDefiningClass(UnmarshallingContext context) throws IOException {
		if (!(StorageTypes.from(context) instanceof ClassType definingClass))
			throw new IOException("The type defining a constructor must be a class type");
	
		return definingClass;
	}

	protected static StorageType[] formalsAsTypes(MethodSignatureJson json) throws InconsistentJsonException {
		var formals = json.getFormals().toArray(String[]::new);
		var formalsAsTypes = new StorageType[formals.length];
		int pos = 0;
		for (var formal: formals)
			formalsAsTypes[pos++] = AbstractStorageType.named(formal, InconsistentJsonException::new);

		return formalsAsTypes;
	}

	@Override
	public final ClassType getDefiningClass() {
		return definingClass;
	}

	@Override
	public final Stream<StorageType> getFormals() {
		return Stream.of(formals);
	}

	/**
	 * Yields a comma-separated string of the formal arguments of the method or constructor, ordered from left to right.
	 * 
	 * @return the string
	 */
	protected final String commaSeparatedFormals() {
		return getFormals()
			.map(StorageType::toString)
			.collect(Collectors.joining(",", "(", ")"));
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof AbstractCodeSignature acs)
			return acs.definingClass.equals(definingClass) && Arrays.equals(acs.formals, formals); // optimization
		else
			return other instanceof CodeSignature cs && cs.getDefiningClass().equals(definingClass)
				&& Arrays.equals(cs.getFormals().toArray(StorageType[]::new), formals);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ Arrays.hashCode(formals);
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return NodeMarshallingContexts.of(os);
	}
}