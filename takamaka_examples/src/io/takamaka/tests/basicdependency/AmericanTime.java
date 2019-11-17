package io.takamaka.tests.basicdependency;

public class AmericanTime extends Time {
	public enum Period {
		AM, PM
	}

	public AmericanTime(int hours, int minutes, int seconds, Period period) {
		super(seconds + 60 * minutes + 3600 * translateHours(period, hours));

		if (hours < 1 || hours > 12 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60)
			throw new IllegalArgumentException();
	}

	private AmericanTime(int secondsFromStartOfDay) {
		super(secondsFromStartOfDay);
	}

	private static int translateHours(Period period, int hours) {
		if (hours == 12)
			hours = 0;

		if (period == Period.AM)
			return hours;
		else
			return hours + 12;
	}

	@Override
	public String toString() {
		Period period;

		int hours = (secondsFromStartOfDay / 3600);
		if (hours < 12)
			period = Period.AM;
		else
			period = Period.PM;

		if (hours == 0)
			hours = 12;
		else if (hours > 12)
			hours -= 12;

		return twoDigits(hours) + ':' + twoDigits((secondsFromStartOfDay / 60) % 60)
			+ ':' + twoDigits(secondsFromStartOfDay % 60) + period;
	}

	@Override
	public AmericanTime after(int minutes) {
		if (minutes < 0)
			throw new IllegalArgumentException();

		return new AmericanTime((secondsFromStartOfDay + minutes * 60) % (24 * 60 * 60));
	}
}