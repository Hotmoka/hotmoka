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