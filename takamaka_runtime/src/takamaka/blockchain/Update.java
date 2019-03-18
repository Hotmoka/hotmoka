package takamaka.blockchain;

import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

@Immutable
public final class Update implements Comparable<Update> {
	public final StorageReference object;
	public final FieldReference field;
	public final StorageValue value;
	private final static String CLASS_TAG_FIELD_NAME = "@class";
	private final static BooleanValue VALUE_FOR_CLASS_TAG = new BooleanValue(true);

	private Update(StorageReference object, FieldReference field, StorageValue value) {
		this.object = object;
		this.field = field;
		this.value = value;
	}

	@Override
	public String toString() {
		return object + ";" + field + ";" + value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Update && ((Update) other).object.equals(object)
				&& ((Update) other).field.equals(field) && ((Update) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return object.hashCode() ^ field.hashCode() ^ value.hashCode();
	}

	public static Update mk(StorageReference object, FieldReference field, StorageValue value) {
		return new Update(object, field, value);
	}

	public static Update mkForClassTag(StorageReference object, String className) {
		return new Update(object, new FieldReference(className, CLASS_TAG_FIELD_NAME, BasicTypes.BOOLEAN), VALUE_FOR_CLASS_TAG);
	}

	public static Update mkFromString(String s) {
		String[] parts = s.split(";");
		if (parts.length != 5)
			throw new IllegalArgumentException("Illegal string format " + s);

		StorageType type = StorageType.of(parts[3]);

		return new Update(new StorageReference(parts[0]),
			new FieldReference(new ClassType(parts[1]), parts[2], type),
			StorageValue.of(type, parts[4]));
	}

	public boolean isClassTag() {
		return CLASS_TAG_FIELD_NAME.equals(field.name);
	}

	@Override
	public int compareTo(Update other) {
		int diff = object.compareTo(other.object);
		if (diff != 0)
			return diff;

		diff = field.compareTo(other.field);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(other.value);
	}
}