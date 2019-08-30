package takamaka.whitelisted.java.lang;

public abstract class StringBuilder {

	public StringBuilder() {
	}

	public StringBuilder(java.lang.String message) {
	}

	public StringBuilder(int i) {
	}

	public abstract java.lang.StringBuilder append(boolean b);
	public abstract java.lang.StringBuilder append(char c);
	public abstract java.lang.StringBuilder append(int i);
	public abstract java.lang.StringBuilder append(long l);
	public abstract java.lang.StringBuilder append(float f);
	public abstract java.lang.StringBuilder append(double d);
	public abstract java.lang.StringBuilder append(java.lang.String s);
	public abstract java.lang.String toString();
}