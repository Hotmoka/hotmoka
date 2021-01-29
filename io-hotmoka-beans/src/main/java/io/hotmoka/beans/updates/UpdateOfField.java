package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that the field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public abstract class UpdateOfField extends Update {

	/**
	 * The field that is modified.
	 */
	public final FieldSignature field;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	protected UpdateOfField(StorageReference object, FieldSignature field) {
		super(object);

		this.field = field;
	}

	/**
	 * Yields the field whose value is updated.
	 *
	 * @return the field
	 */
	public final FieldSignature getField() {
		return field;
	}

	/**
	 * Yields the value set into the updated field.
	 * 
	 * @return the value
	 */
	public abstract StorageValue getValue();

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfField && super.equals(other) && ((UpdateOfField) other).field.equals(field);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ field.hashCode();
	}

	@Override
	public final String toString() {
		return "<" + object + "|" + getField() + "|" + getValue() + ">";
	}

	@Override
	public final boolean sameProperty(Update other) {
		return other instanceof UpdateOfField && getField().equals(((UpdateOfField) other).getField());
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return field.compareTo(((UpdateOfField) other).field);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(field.size(gasCostModel));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		super.into(context);
		field.into(context);
	}

	protected final void intoWithoutField(MarshallingContext context) throws IOException {
		super.into(context);
	}
}