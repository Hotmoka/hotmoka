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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.NodeMarshallingContexts;
import io.hotmoka.beans.api.signatures.CodeSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;

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

	@Override
	public final ClassType getDefiningClass() {
		return definingClass;
	}

	/**
	 * Yields the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the formal arguments
	 */
	public final Stream<StorageType> getFormals() {
		return Stream.of(formals);
	}

	/**
	 * Yields a comma-separated string of the formal arguments of the method or constructor, ordered left to right.
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
			return other instanceof CodeSignature cs && cs.getDefiningClass().equals(definingClass) && Arrays.equals(cs.getFormals().toArray(StorageType[]::new), formals);
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