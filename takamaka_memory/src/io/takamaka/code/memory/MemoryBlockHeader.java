package io.takamaka.code.memory;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MemoryBlockHeader implements Serializable {
	private static final long serialVersionUID = 6163345302977772036L;

	/**
	 * The time of creation of the block, as returned by {@link java.lang.System#currentTimeMillis()}.
	 */
	public final long time;

	/**
	 * Builds block header.
	 * 
	 * @param time the time of creation of the block, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	protected MemoryBlockHeader(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		Date date = new Date(time);
		DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateFormatted = formatter.format(date);

		return "block creation time: " + time + " [" + dateFormatted + " UTC]";
	}
}