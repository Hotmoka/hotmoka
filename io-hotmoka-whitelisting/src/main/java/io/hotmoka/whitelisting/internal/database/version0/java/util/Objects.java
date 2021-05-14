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

package io.hotmoka.whitelisting.internal.database.version0.java.util;

import io.hotmoka.whitelisting.HasDeterministicTerminatingToString;

public interface Objects {
	<T> T requireNonNull(T obj);
	<T> T requireNonNull(T obj, java.lang.String message);
	boolean isNull(java.lang.Object obj);
	boolean nonNull(java.lang.Object obj);
	<T> T requireNonNullElse(T obj, T defaultObj);
	java.lang.String toString(@HasDeterministicTerminatingToString java.lang.Object o);
}