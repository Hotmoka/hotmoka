/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.examples.basicdependency;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.lang.View;

public class AmericanTime extends Time {

	public class Period extends Storage {
		private final String s;

		private Period(String s) {
			this.s = s;
		}

		@Override
		public String toString() {
			return s;
		}
	}

	private final Period AM = new Period("AM");
	private final Period PM = new Period("PM");

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

		if (StringSupport.equals(period.s, "AM"))
			return hours;
		else
			return hours + 12;
	}

	@Override
	public @View String toString() {
		Period period;

		int hours = (secondsFromStartOfDay / 3600);
		if (hours < 12)
			period = AM;
		else
			period = PM;

		if (hours == 0)
			hours = 12;
		else if (hours > 12)
			hours -= 12;

		return StringSupport.concat(twoDigits(hours), ':', twoDigits((secondsFromStartOfDay / 60) % 60), ':', twoDigits(secondsFromStartOfDay % 60), period);
	}

	@Override
	public AmericanTime after(int minutes) {
		if (minutes < 0)
			throw new IllegalArgumentException();

		return new AmericanTime((secondsFromStartOfDay + minutes * 60) % (24 * 60 * 60));
	}
}