package io.takamaka.code.whitelisting.internal.database.java.util.stream;

public interface Collectors {
	java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String>
		joining(java.lang.CharSequence delimiter, java.lang.CharSequence prefix, java.lang.CharSequence suffix);
	java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String> joining(java.lang.CharSequence delimiter);
	java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String> joining();
}