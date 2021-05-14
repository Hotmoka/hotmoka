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

package io.hotmoka.xodus.env;

public class Transaction {
	private final jetbrains.exodus.env.Transaction parent;

	Transaction(jetbrains.exodus.env.Transaction parent) {
		this.parent = parent;
	}

	public jetbrains.exodus.env.Transaction toNative() {
		return parent;
	}

	public boolean isFinished() {
		return parent.isFinished();
	}

	public void abort() {
		parent.abort();
	}

	public boolean commit() {
		return parent.commit();
	}
}