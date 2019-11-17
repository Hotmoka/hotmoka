package io.takamaka.tests.basicdependency;

public class InternationalTime extends Time {

	public InternationalTime(int hours, int minutes, int seconds) {
		super(seconds + 60 * minutes + 3600 * hours);

		if (hours < 0 || hours >= 24 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60)
			throw new IllegalArgumentException();
	}

	private InternationalTime(int secondsFromStartOfDay) {
		super(secondsFromStartOfDay);
	}

	@Override
	public String toString() {
		return twoDigits(secondsFromStartOfDay / 3600) + ':' + twoDigits((secondsFromStartOfDay / 60) % 60)
			+ ':' + twoDigits(secondsFromStartOfDay % 60);
	}

	@Override
	public InternationalTime after(int minutes) {
		if (minutes < 0)
			throw new IllegalArgumentException();

		return new InternationalTime((secondsFromStartOfDay + minutes * 60) % (24 * 60 * 60));
	}
}