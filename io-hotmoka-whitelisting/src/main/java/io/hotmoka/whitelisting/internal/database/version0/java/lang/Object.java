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

package io.hotmoka.whitelisting.internal.database.version0.java.lang;

import io.hotmoka.whitelisting.HasDeterministicTerminatingEquals;
import io.hotmoka.whitelisting.HasDeterministicTerminatingHashCode;
import io.hotmoka.whitelisting.HasDeterministicTerminatingToString;

public abstract class Object {
	public Object() {}
	public abstract java.lang.Object clone();
	//public abstract java.lang.Class<?> getClass(); // this needs a special treatment in the code since it's final in Object
	public abstract @HasDeterministicTerminatingEquals boolean equals(java.lang.Object other);
	public abstract @HasDeterministicTerminatingToString java.lang.String toString();
	public abstract @HasDeterministicTerminatingHashCode int hashCode();
}