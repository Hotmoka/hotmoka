package io.takamaka.tests.allocations;

import io.takamaka.code.lang.Storage;

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
		Allocations[][][][] a5 = new Allocations[f][][][];
		int[][][][] a6 = new int[f][f][f + 2][f * 3];
		int[][][][] a7 = new int[f][f][][];
		int[][][][] a8 = new int[f][][][];
		a6[1][2][3][4] = 15;
		a7[1][2] = null;
		this.f = a1.length + a2.length + a3.length + a4.length + a5.length + a8.length;
	}

	private Allocations(int f) {
		this.f = f;
	}
}