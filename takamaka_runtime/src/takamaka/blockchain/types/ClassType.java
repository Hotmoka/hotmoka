package takamaka.blockchain.types;

import takamaka.lang.Immutable;

@Immutable
public final class ClassType implements StorageType {
	public final String name;

	public ClassType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Class<?> toClass() throws ClassNotFoundException {
		return Class.forName(name);
	}
}