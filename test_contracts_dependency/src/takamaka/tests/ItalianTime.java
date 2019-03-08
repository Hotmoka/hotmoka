package takamaka.tests;

import takamaka.lang.Entry;

public class ItalianTime extends Time {

	public @Entry ItalianTime(int hours, int minutes, int seconds) {
		super(seconds + 60 * minutes + 3600 * hours);

		if (hours < 0 || hours >= 24 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60)
			throw new IllegalArgumentException();
	}

	private ItalianTime(int secondsFromStartOfDay) {
		super(secondsFromStartOfDay);
	}

	@Override
	public String toString() {
		return String.format("%02d:%02d:%02d", secondsFromStartOfDay / 3600, (secondsFromStartOfDay / 60) % 60, secondsFromStartOfDay % 60);
	}

	@Override
	public ItalianTime after(int minutes) {
		if (minutes < 0)
			throw new IllegalArgumentException();

		return new ItalianTime((secondsFromStartOfDay + minutes * 60) % (24 * 60 * 60));
	}
}