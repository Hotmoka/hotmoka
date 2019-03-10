package takamaka.blockchain;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Immutable;

@Immutable
public abstract class CodeReference {
	public final ClassType definingClass;
	private final StorageType[] formals;

	public CodeReference(ClassType definingClass, StorageType... formals) {
		this.definingClass = definingClass;
		this.formals = formals;
	}

	public CodeReference(String definingClass, StorageType... formals) {
		this(new ClassType(definingClass), formals);
	}

	public Stream<StorageType> formals() {
		return Stream.of(formals);
	}

	protected final String commaSeparatedFormals() {
		return formals()
			.map(StorageType::toString)
			.collect(Collectors.joining(",", "(", ")"));
	}
}