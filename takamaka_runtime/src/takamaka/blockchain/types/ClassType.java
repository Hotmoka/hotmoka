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

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassType && ((ClassType) other).name.equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}