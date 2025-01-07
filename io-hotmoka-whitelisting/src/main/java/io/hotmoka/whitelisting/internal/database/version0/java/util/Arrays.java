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

public interface Arrays {
	boolean equals(byte[] a1, byte[] a2);
	boolean equals(boolean[] a1, boolean[] a2);
	boolean equals(char[] a1, char[] a2);
	boolean equals(int[] a1, int[] a2);
	boolean equals(float[] a1, float[] a2);
	boolean equals(long[] a1, long[] a2);
	boolean equals(double[] a1, double[] a2);
	boolean equals(short[] a1, short[] a2);
}