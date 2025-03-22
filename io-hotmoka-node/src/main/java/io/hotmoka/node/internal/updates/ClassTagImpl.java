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

package io.hotmoka.node.internal.updates;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.UpdateJson;
import io.hotmoka.node.internal.types.ClassTypeImpl;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of an update that states that an object belongs to a given class.
 */
@Immutable
public final class ClassTagImpl extends AbstractUpdate implements ClassTag {
	final static byte SELECTOR = 0;

	/**
	 * The class of the object.
	 */
	private final ClassType clazz;

	/**
	 * The reference to the transaction that installed the jar from which the class was resolved.
	 */
	private final TransactionReference jar;

	/**
	 * Builds an update for the class tag of an object.
	 * 
	 * @param object the storage reference of the object whose class name is set
	 * @param clazz the class of the object
	 * @param jar the reference to the transaction that installed the jar from which the class was resolved
	 */
	public ClassTagImpl(StorageReference object, ClassType clazz, TransactionReference jar) {
		this(object, clazz, jar, IllegalArgumentException::new);
	}

	/**
	 * Builds an update for the class tag of an object from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public ClassTagImpl(UpdateJson json) throws InconsistentJsonException {
		this(
			unmapObject(json),
			ClassTypeImpl.named(json.getClazz(), InconsistentJsonException::new),
			Objects.requireNonNull(json.getJar(), "jar cannot be null if clazz is non-null", InconsistentJsonException::new).unmap(),
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals an update for the class tag of an object from the given context.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the unmarshalling failed
	 */
	public ClassTagImpl(UnmarshallingContext context) throws IOException {
		this(
			StorageReferenceImpl.fromWithoutSelector(context),
			unmarshalClass(context),
			TransactionReferences.from(context),
			IOException::new
		);
	}

	/**
	 * Builds an update for the class tag of an object.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param object the storage reference of the object whose class name is set
	 * @param clazz the class of the object
	 * @param jar the reference to the transaction that installed the jar from which the class was resolved
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> ClassTagImpl(StorageReference object, ClassType clazz, TransactionReference jar, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(object, onIllegalArgs);
	
		this.jar = Objects.requireNonNull(jar, "jar cannot be null", onIllegalArgs);
		this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null", onIllegalArgs);
	}

	private static ClassType unmarshalClass(UnmarshallingContext context) throws IOException {
		if (StorageTypes.from(context) instanceof ClassType clazz)
			return clazz;
		else
			throw new IOException("A class tag must refer to a class type");
	}

	@Override
	public ClassType getClazz() {
		return clazz;
	}

	@Override
	public TransactionReference getJar() {
		return jar;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassTag ct && super.equals(other) && ct.getClazz().equals(clazz) && ct.getJar().equals(jar);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ clazz.hashCode() ^ jar.hashCode();
	}

	@Override
	public String toString() {
		return "<" + getObject() + ".class|" + clazz + "|@" + jar + ">";
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;

		diff = clazz.compareTo(((ClassTagImpl) other).clazz);
		if (diff != 0)
			return diff;
		else
			return jar.compareTo(((ClassTagImpl) other).jar);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		clazz.into(context);
		jar.into(context);
	}

	@Override
	public boolean sameProperty(Update other) {
		return other instanceof ClassTag;
	}
}