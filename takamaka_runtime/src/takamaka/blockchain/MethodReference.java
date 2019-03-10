package takamaka.blockchain;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Immutable;

@Immutable
public final class MethodReference extends CodeReference {
	public final String methodName;

	public MethodReference(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, formals);

		this.methodName = methodName;
	}

	public MethodReference(String definingClass, String methodName, StorageType... formals) {
		this(new ClassType(definingClass), methodName, formals);
	}

	@Override
	public String toString() {
		return definingClass + "." + methodName + commaSeparatedFormals();
	};
}