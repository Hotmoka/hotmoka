package takamaka.tests.errors.illegaltypeforstoragefield3;

/**
 * An enumeration with instance fields.
 */
public enum MyEnum {
	FIRST, SECOND, THIRD;

	private int i;

	public void set(int i) {
		this.i = i;
	}

	@Override
	public String toString() {
		return "I am " + i;
	}
}