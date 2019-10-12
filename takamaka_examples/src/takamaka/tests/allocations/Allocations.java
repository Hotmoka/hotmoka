package takamaka.tests.allocations;

import takamaka.lang.Storage;

public class Allocations extends Storage {
	public int f;

	public Allocations() {
		this.f = 17;

		new String("hello");
		new Allocations(13);
		Allocations[] a1 = new Allocations[f];
		int[] a2 = new int[f];
		Allocations[][][][] a3 = new Allocations[f][f][f + 2][f * 3];
		Allocations[][][][] a4 = new Allocations[f][f][][];
		this.f = a1.length + a2.length + a3.length + a4.length;
	}

	private Allocations(int f) {
		this.f = f;
	}
}