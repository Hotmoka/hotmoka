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

package io.takamaka.code.whitelisting.internal.database.version0.java.util;

public interface Comparator<T> {
	<D> java.util.Comparator<D> comparingInt(java.util.function.ToIntFunction<? super D> keyExtractor);
	<D> java.util.Comparator<D> comparingLong(java.util.function.ToLongFunction<? super D> keyExtractor);
	<D> java.util.Comparator<D> comparingDouble(java.util.function.ToDoubleFunction<? super D> keyExtractor);
	<T1, U extends java.lang.Comparable<? super U>> java.util.Comparator<T1> comparing(java.util.function.Function<? super T1, ? extends U> extractor);
}