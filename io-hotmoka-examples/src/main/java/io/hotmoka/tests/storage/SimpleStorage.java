package io.hotmoka.tests.storage;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

@Exported
public class SimpleStorage extends Storage {
	private int storedData;
	
	public void set(int x) {
		storedData = x;
	}

	public @View int get() {
		return storedData;
	}
}