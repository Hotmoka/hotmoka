package takamaka.blockchain;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Immutable;

@Immutable
public final class FieldReference {
	public final ClassType definingClass;
	public final String name;
	public final StorageType type;

	FieldReference(ClassType definingClass, String name, StorageType type) {
		this.definingClass = definingClass;
		this.name = name;
		this.type = type;
	}
}