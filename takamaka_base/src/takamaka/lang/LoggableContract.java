package takamaka.lang;

import java.util.Arrays;

import takamaka.util.StorageList;

public abstract class LoggableContract extends Contract {
	private final StorageList<String> logs;

	protected LoggableContract() {
		this.logs = new StorageList<>();
	}

	protected final void log(String tag, Object... objects) {
		logs.add(tag + ": " + Arrays.toString(objects));
	}
}