package takamaka.whitelisted.java.util.stream;

public abstract class Collectors {
	public abstract java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String>
		joining(java.lang.CharSequence delimiter, java.lang.CharSequence prefix, java.lang.CharSequence suffix);
	public abstract java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String> joining(java.lang.CharSequence delimiter);
}