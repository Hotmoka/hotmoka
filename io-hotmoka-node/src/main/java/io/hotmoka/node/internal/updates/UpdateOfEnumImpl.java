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
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfEnum;
import io.hotmoka.node.api.values.EnumValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The implementation of an update of a field of enumeration type.
 */
@Immutable
public final class UpdateOfEnumImpl extends UpdateOfFieldImpl implements UpdateOfEnum {
	final static byte SELECTOR_EAGER = 8;
	final static byte SELECTOR_LAZY = 9;

	/**
	 * The name of the enumeration class whose element is being assigned to the field.
	 */
	private final String enumClassName;

	/**
	 * The name of the enumeration value put as new value of the field.
	 */
	private final String name;

	/**
	 * True if and only if the update is eager.
	 */
	private final boolean eager;

	/**
	 * Builds an update of an enumeration field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param enumClassName the name of the enumeration class whose element is being assigned to the field
	 * @param name the name of the enumeration value put as new value of the field
	 * @param eager true if and only if the update is eager
	 */
	public UpdateOfEnumImpl(StorageReference object, FieldSignature field, String enumClassName, String name, boolean eager) {
		super(object, field);

		this.enumClassName = Objects.requireNonNull(enumClassName, "enumClassName cannot be null");
		this.name = Objects.requireNonNull(name, "name cannot be null");
		this.eager = eager;
	}

	@Override
	public EnumValue getValue() {
		return StorageValues.enumElementOf(enumClassName, name);
	}

	@Override
	public String getEnumClassName() {
		return enumClassName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfEnum uoee && super.equals(other) && uoee.isEager() == eager
			&& uoee.getEnumClassName().equals(enumClassName) && uoee.getName().equals(name);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ name.hashCode() ^ enumClassName.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;

		diff = enumClassName.compareTo(((UpdateOfEnumImpl) other).enumClassName);
		if (diff != 0)
			return diff;

		diff = name.compareTo(((UpdateOfEnumImpl) other).name);
		if (diff != 0)
			return diff;

		return Boolean.compare(eager, ((UpdateOfEnumImpl) other).eager);
	}

	@Override
	public boolean isEager() {
		return eager;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(eager ? SELECTOR_EAGER : SELECTOR_LAZY);
		super.into(context);
		context.writeStringUnshared(enumClassName);
		context.writeStringUnshared(name);
	}
}