package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class CallerOutsideEntry extends Error {

	public CallerOutsideEntry(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "caller() can only be used inside an @Entry method or constructor");
	}
}