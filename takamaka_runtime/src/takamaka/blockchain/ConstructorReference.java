package takamaka.blockchain;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Immutable;

@Immutable
public final class ConstructorReference extends CodeReference {

	public ConstructorReference(ClassType definingClass, StorageType... formals) {
		super(definingClass, formals);
	}

	public ConstructorReference(String definingClass, StorageType... formals) {
		super(definingClass, formals);
	}

	@Override
	public String toString() {
		return definingClass + commaSeparatedFormals();
	};
}