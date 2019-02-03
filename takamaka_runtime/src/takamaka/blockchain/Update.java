package takamaka.blockchain;

import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.values.StorageValue;
import takamaka.blockchain.values.StringValue;
import takamaka.lang.Immutable;

@Immutable
public final class Update {
	public final StorageReference object;
	public final FieldReference field;
	public final StorageValue value;
	private final static String CLASS_TAG_FIELD_NAME = "@class";

	private Update(StorageReference object, FieldReference field, StorageValue value) {
		this.object = object;
		this.field = field;
		this.value = value;
	}

	public static Update mk(StorageReference object, FieldReference field, StorageValue value) {
		return new Update(object, field, value);
	}

	public static Update mkForClassTag(StorageReference object, String className) {
		return new Update(object, new FieldReference(className, CLASS_TAG_FIELD_NAME, BasicTypes.STRING), new StringValue(className));
	}
}