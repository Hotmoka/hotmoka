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

package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;

/**
 * An update that states that an object belongs to a given class.
 * It is stored in blockchain by the transaction that created the
 * object and is not modified later anymore.
 */
@Immutable
public final class ClassTag extends Update {
	final static byte SELECTOR = 0;

	/**
	 * The class of the object.
	 */
	public final ClassType clazz;

	/**
	 * The transaction that installed the jar from which the class was resolved.
	 */
	public final TransactionReference jar;

	/**
	 * Builds an update for the class tag of an object.
	 * 
	 * @param object the storage reference of the object whose class name is set
	 * @param className the name of the class of the object
	 * @param jar the transaction that installed the jar from which the class was resolved
	 */
	public ClassTag(StorageReference object, String className, TransactionReference jar) {
		super(object);

		this.clazz = new ClassType(className);
		this.jar = jar;
	}

	/**
	 * Builds an update for the class tag of an object.
	 * 
	 * @param object the storage reference of the object whose class name is set
	 * @param clazz the class of the object
	 * @param jar the transaction that installed the jar from which the class was resolved
	 */
	public ClassTag(StorageReference object, ClassType clazz, TransactionReference jar) {
		super(object);

		this.clazz = clazz;
		this.jar = jar;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassTag && super.equals(other)
			&& ((ClassTag) other).clazz.equals(clazz)
			&& ((ClassTag) other).jar.equals(jar);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ clazz.hashCode() ^ jar.hashCode();
	}

	@Override
	public String toString() {
		return "<" + object + ".class|" + clazz + "|@" + jar + ">";
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;

		diff = clazz.compareAgainst(((ClassTag) other).clazz);
		if (diff != 0)
			return diff;
		else
			return jar.compareTo(((ClassTag) other).jar);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(clazz.size(gasCostModel)).add(gasCostModel.storageCostOf(jar));
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