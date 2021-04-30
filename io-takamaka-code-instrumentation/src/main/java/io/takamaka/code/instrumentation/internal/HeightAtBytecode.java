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

package io.takamaka.code.instrumentation.internal;

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