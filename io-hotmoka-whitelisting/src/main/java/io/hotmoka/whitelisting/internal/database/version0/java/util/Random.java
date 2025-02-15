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

/**
 * White-listed only if created with seed.
 */
public abstract class Random {
	public Random(long seed) {}

	public abstract void setSeed(long seed);
	public abstract int nextInt();
	public abstract int nextInt(int bound);
	public abstract long nextLong();
	public abstract boolean nextBoolean();
	public abstract float nextFloat();
	public abstract double nextDouble();
	public abstract double nextGaussian();
}