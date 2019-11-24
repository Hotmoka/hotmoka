package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import java.lang.reflect.Field;

import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;

/**
 * An instrumentation that adds fields for the old value and the loading state of the fields of a storage class.
 */
public class AddOldAndIfAlreadyLoadedFields extends InstrumentedClassImpl.Builder.ClassLevelInstrumentation {

	public AddOldAndIfAlreadyLoadedFields(InstrumentedClassImpl.Builder builder) {
		builder.super();

		if (isStorage) {
			eagerNonTransientInstanceFields.getLast().forEach(this::addOldFieldFor);
			lazyNonTransientInstanceFields.forEach(this::addOldFieldFor);
			lazyNonTransientInstanceFields.forEach(this::addIfAlreadyLoadedFieldFor);
		}
	}

	/**
	 * Adds the field for the old value of a field of a storage class.
	 * 
	 * @param field the field of the storage class
	 */
	private void addOldFieldFor(Field field) {
		addField(new FieldGen(InstrumentedClassImpl.PRIVATE_SYNTHETIC_TRANSIENT, Type.getType(field.getType()),
			InstrumentedClassImpl.OLD_PREFIX + field.getName(), cpg).getField());
	}

	/**
	 * Adds the field for the loading state of a field of a storage class.
	 * 
	 * @param field the field of the storage class
	 */
	private void addIfAlreadyLoadedFieldFor(Field field) {
		addField(new FieldGen(InstrumentedClassImpl.PRIVATE_SYNTHETIC_TRANSIENT, BasicType.BOOLEAN,
			InstrumentedClassImpl.IF_ALREADY_LOADED_PREFIX + field.getName(), cpg).getField());
	}
}