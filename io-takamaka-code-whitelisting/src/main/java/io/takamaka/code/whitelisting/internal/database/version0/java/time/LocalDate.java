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

package io.takamaka.code.whitelisting.internal.database.version0.java.time;

public interface LocalDate {
	java.time.LocalDate of(int year, int month, int day);
	java.time.LocalDate ofInstant(java.time.Instant instant, java.time.ZoneId zone);
	java.time.LocalDate plusDays(long howMany);
	int getDayOfMonth();
	int getMonthValue();
	int getYear();
	boolean isBefore(java.time.chrono.ChronoLocalDate other);
	boolean isAfter(java.time.chrono.ChronoLocalDate other);
	boolean isEqual(java.time.chrono.ChronoLocalDate other);
}