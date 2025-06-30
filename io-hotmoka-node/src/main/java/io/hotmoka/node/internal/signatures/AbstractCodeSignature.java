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

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.node.api.signatures.CodeSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.json.MethodSignatureJson;
import io.hotmoka.node.internal.types.AbstractStorageType;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The signature of a method or constructor.
 */
@Immutable
public abstract class AbstractCodeSignature extends AbstractSignature implements CodeSignature {

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
	protected <E extends Exception> AbstractCodeSignature(ClassType definingClass, StorageType[] formals, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(definingClass, onIllegalArgs);
		
		this.formals = Objects.requireNonNull(formals, "formals cannot be null", onIllegalArgs);
		for (var formal: formals)
			Objects.requireNonNull(formal, "formals cannot hold null", onIllegalArgs);
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
			return super.equals(other) && Arrays.equals(acs.formals, formals); // optimization
		else
			return other instanceof CodeSignature cs && super.equals(other)
				&& Arrays.equals(cs.getFormals().toArray(StorageType[]::new), formals);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(formals);
	}
}