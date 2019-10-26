package takamaka.tests.storage;
import io.takamaka.code.annotations.View;
import io.takamaka.code.lang.Storage;

public class SimpleStorage extends Storage {
	private int storedData;
	
	public void set(int x) {
		storedData = x;
	}

	public @View int get() {
		return storedData;
	}
}