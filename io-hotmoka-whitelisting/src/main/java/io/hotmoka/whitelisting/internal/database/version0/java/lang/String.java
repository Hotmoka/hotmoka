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

public abstract class String {
	public String(java.lang.String original) {}
	public String(byte[] bytes, java.nio.charset.Charset charSet) {}
	public abstract int length();
	public abstract boolean isEmpty();
	public abstract boolean equals(java.lang.Object other);
	public abstract int compareTo(java.lang.String other);
	public abstract java.lang.String toString();
	public abstract java.lang.String valueOf(long l);
	public abstract java.lang.String valueOf(int i);
	public abstract java.lang.String valueOf(char c);
	public abstract java.lang.String valueOf(short s);
	public abstract java.lang.String valueOf(double d);
	public abstract java.lang.String valueOf(float f);
	public abstract java.lang.String valueOf(boolean b);
	public abstract java.lang.String valueOf(byte b);
	public abstract java.lang.String concat(java.lang.String other);
	public abstract boolean endsWith(java.lang.String suffix);
	public abstract boolean startsWith(java.lang.String prefix);
	public abstract java.lang.String toLowerCase();
	public abstract java.lang.String toUpperCase();
	public abstract int indexOf(int c);
	public abstract java.lang.String substring(int begin, int end);
	public abstract java.lang.String substring(int begin);
	public abstract char charAt(int pos);
}