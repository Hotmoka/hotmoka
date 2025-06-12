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

package io.hotmoka.tutorial.examples.family;

import io.takamaka.code.lang.StringSupport;

public class Person {
	private final String name;
	private final int day;
	private final int month;
	private final int year;
	public final Person parent1;
	public final Person parent2;

	public Person(String name, int day, int month, int year,
			      Person parent1, Person parent2) {

		this.name = name;
		this.day = day;
		this.month = month;
		this.year = year;
		this.parent1 = parent1;
		this.parent2 = parent2;
	}

	public Person(String name, int day, int month, int year) {
		this(name, day, month, year, null, null);
	}

	@Override
	public String toString() {
		return StringSupport.concat(name, " (", day, "/", month, "/", year, ")");
	}
}