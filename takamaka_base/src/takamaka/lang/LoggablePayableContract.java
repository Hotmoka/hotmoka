package takamaka.lang;

import java.util.Arrays;

import takamaka.util.StorageList;

public abstract class LoggablePayableContract extends PayableContract {
	private final StorageList<String> logs = new StorageList<>();

	protected final void log(String tag, Object... objects) {
		logs.add(tag + ": " + Arrays.toString(objects));
	}
}