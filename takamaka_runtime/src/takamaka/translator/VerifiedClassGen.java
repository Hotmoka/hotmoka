package takamaka.translator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;

/**
 * A BCEL class generator, specialized in order to verify some constraints required by Takamaka.
 */
public class VerifiedClassGen extends ClassGen implements Comparable<VerifiedClassGen> {

	/**
	 * The class loader used to load this class and the other classes of the program
	 * it belongs to.
	 */
	private final TakamakaClassLoader classLoader;

	public VerifiedClassGen(JavaClass clazz, TakamakaClassLoader classLoader) {
		super(clazz);

		this.classLoader = classLoader;

		verify();
	}

	@Override
	public int compareTo(VerifiedClassGen other) {
		return getClassName().compareTo(other.getClassName());
	}

	private void verify() {
		
	}
}