package takamaka.tests.storage;
import io.takamaka.lang.Storage;
import takamaka.lang.View;

public class SimpleStorage extends Storage {
	private int storedData;
	
	public void set(int x) {
		storedData = x;
	}

	public @View int get() {
		return storedData;
	}
}