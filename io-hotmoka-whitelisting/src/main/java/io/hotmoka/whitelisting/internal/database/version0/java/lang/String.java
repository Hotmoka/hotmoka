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

/**
 * White-listed methods of String. Other methods, with
 * non-constant complexity, are inside {@link io.takamaka.code.lang.StringSupport}.
 */
public abstract class String {
	public abstract int length();
	public abstract boolean isEmpty();
	public abstract java.lang.String toString();
	public abstract java.lang.String valueOf(long l);
	public abstract java.lang.String valueOf(int i);
	public abstract java.lang.String valueOf(char c);
	public abstract java.lang.String valueOf(short s);
	public abstract java.lang.String valueOf(double d);
	public abstract java.lang.String valueOf(float f);
	public abstract java.lang.String valueOf(boolean b);
	public abstract java.lang.String valueOf(byte b);
	public abstract char charAt(int pos);
}