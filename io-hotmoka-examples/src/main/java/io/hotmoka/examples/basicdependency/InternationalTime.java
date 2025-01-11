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

import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.lang.View;

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
	public @View String toString() {
		return StringSupport.concat(twoDigits(secondsFromStartOfDay / 3600), ':', twoDigits((secondsFromStartOfDay / 60) % 60), ':', twoDigits(secondsFromStartOfDay % 60));
	}

	@Override
	public InternationalTime after(int minutes) {
		if (minutes < 0)
			throw new IllegalArgumentException();

		return new InternationalTime((secondsFromStartOfDay + minutes * 60) % (24 * 60 * 60));
	}
}