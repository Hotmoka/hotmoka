package takamaka.blockchain;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * The header of a block. It contains data specific to the
 * implementation of a blockchain. At least the time of creation
 * of the block must be included.
 */
public abstract class BlockHeader implements Serializable {
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
	protected BlockHeader(long time) {
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