package takamaka.tests;

import takamaka.lang.Storage;

public abstract class Time extends Storage implements Comparable<Time> {
	protected final int secondsFromStartOfDay;

	protected Time(int secondsFromStartOfDay) {
		this.secondsFromStartOfDay = secondsFromStartOfDay;
	}

	public abstract Time after(int minutes);

	public boolean isBeforeOrEqualTo(Time other) {
		return compareTo(other) <= 0;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Time && ((Time) other).secondsFromStartOfDay == secondsFromStartOfDay;
	}

	@Override
	public int hashCode() {
		return secondsFromStartOfDay;
	}

	@Override
	public int compareTo(Time other) {
		return secondsFromStartOfDay - other.secondsFromStartOfDay;
	}

	public abstract String toString();
}