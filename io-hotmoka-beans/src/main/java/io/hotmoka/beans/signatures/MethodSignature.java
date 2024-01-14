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

package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * The signature of a method of a class.
 */
@Immutable
public abstract class MethodSignature extends CodeSignature {

	/**
	 * The name of the method.
	 */
	public final String methodName;

	/**
	 * Builds the signature of a method.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	protected MethodSignature(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, formals);

		this.methodName = Objects.requireNonNull(methodName, "methodName cannot be null");
	}

	@Override
	public String toString() {
		return definingClass + "." + methodName + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof MethodSignature ms && methodName.equals(ms.methodName) && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ methodName.hashCode();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		super.into(context);
		context.writeStringUnshared(methodName);
	}
}