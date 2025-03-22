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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.Signature;
import io.hotmoka.node.api.types.ClassType;

/**
 * The signature of a field, method or constructor.
 */
@Immutable
public abstract class AbstractSignature extends AbstractMarshallable implements Signature {

	/**
	 * The class defining the field, method or constructor.
	 */
	private final ClassType definingClass;

	/**
	 * Builds the signature of a field, method or constructor.
	 * 
	 * @param <E> the type of the exception thrown if some arguments is illegal
	 * @param definingClass the class defining the field, method or constructor
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	protected <E extends Exception> AbstractSignature(ClassType definingClass, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.definingClass = Objects.requireNonNull(definingClass, "definingClass cannot be null", onIllegalArgs);
	}

	protected static ClassType unmarshalDefiningClass(UnmarshallingContext context) throws IOException {
		if (!(StorageTypes.from(context) instanceof ClassType definingClass))
			throw new IOException("The type defining a constructor must be a class type");
	
		return definingClass;
	}

	@Override
	public final ClassType getDefiningClass() {
		return definingClass;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Signature acs && definingClass.equals(acs.getDefiningClass());
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode();
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return NodeMarshallingContexts.of(os);
	}
}