package io.takamaka.code.instrumentation.internal;

import org.apache.bcel.generic.InstructionHandle;

class HeightAtBytecode {
	final InstructionHandle ih;
	final int stackHeightBeforeBytecode;

	HeightAtBytecode(InstructionHandle ih, int stackHeightBeforeBytecode) {
		this.ih = ih;
		this.stackHeightBeforeBytecode = stackHeightBeforeBytecode;
	}

	@Override
	public String toString() {
		return ih + " with " + stackHeightBeforeBytecode + " stack elements";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof HeightAtBytecode && ((HeightAtBytecode) other).ih == ih
			&& ((HeightAtBytecode) other).stackHeightBeforeBytecode == stackHeightBeforeBytecode;
	}

	@Override
	public int hashCode() {
		return ih.getPosition() ^ stackHeightBeforeBytecode;
	}
}