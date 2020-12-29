package io.takamaka.code.verification.internal;

import org.apache.bcel.generic.InstructionHandle;

public class HeightAtBytecode {
	public final InstructionHandle ih;
	public final int stackHeightBeforeBytecode;

	public HeightAtBytecode(InstructionHandle ih, int stackHeightBeforeBytecode) {
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