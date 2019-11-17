package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import java.lang.reflect.Field;

import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.internal.ClassInstrumentation;

/**
 * An instrumentation that adds fields for the old value and the loading state of the fields of a storage class.
 */
public class AddOldAndIfAlreadyLoadedFields {

	public AddOldAndIfAlreadyLoadedFields(ClassInstrumentation.Instrumenter instrumenter) {
		instrumenter.eagerNonTransientInstanceFields.getLast().forEach(field -> addOldFieldFor(field, instrumenter));

		instrumenter.lazyNonTransientInstanceFields.forEach(field -> {
			addOldFieldFor(field, instrumenter);
			addIfAlreadyLoadedFieldFor(field, instrumenter);
		});
	}

	/**
	 * Adds the field for the old value of the fields of a storage class.
	 */
	private void addOldFieldFor(Field field, ClassInstrumentation.Instrumenter instrumenter) {
		instrumenter.clazz.addField(new FieldGen(ClassInstrumentation.PRIVATE_SYNTHETIC_TRANSIENT, Type.getType(field.getType()),
			ClassInstrumentation.OLD_PREFIX + field.getName(), instrumenter.cpg).getField());
	}

	/**
	 * Adds the field for the loading state of the fields of a storage class.
	 */
	private void addIfAlreadyLoadedFieldFor(Field field, ClassInstrumentation.Instrumenter instrumenter) {
		instrumenter.clazz.addField(new FieldGen(ClassInstrumentation.PRIVATE_SYNTHETIC_TRANSIENT, BasicType.BOOLEAN,
			ClassInstrumentation.IF_ALREADY_LOADED_PREFIX + field.getName(), instrumenter.cpg).getField());
	}
}