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

package io.takamaka.code.whitelisting.internal.database.version0.java.lang;

public abstract class Enum {
	public Enum(java.lang.String name, int ordinal) {}
	public abstract java.lang.Enum<?> valueOf(java.lang.Class<?> clazz, java.lang.String name);
	public abstract int ordinal();
	public abstract boolean equals(java.lang.Object other);
	public abstract int hashCode();
	public abstract java.lang.String toString();
}