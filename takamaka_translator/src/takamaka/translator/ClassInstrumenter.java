package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.generic.ClassGen;

class ClassInstrumenter {
	private final JarOutputStream instrumentedJar;

	ClassInstrumenter(JarOutputStream instrumentedJar) {
		this.instrumentedJar = instrumentedJar;
	}

	void addInstrumentationOf(InputStream input, String entryName) throws IOException {
		System.out.println("Instrumenting " + entryName);
		ClassGen classGen = new ClassGen(new ClassParser(input, entryName).parse());
		instrument(classGen);
		classGen.getJavaClass().dump(instrumentedJar);
	}

	private void instrument(ClassGen classGen) {
		//TODO
	}
}