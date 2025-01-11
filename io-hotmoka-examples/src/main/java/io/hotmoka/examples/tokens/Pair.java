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

package io.hotmoka.examples.tokens;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.lang.View;

/**
 * Represents a pair of two elements (standard implementation)
 *
 * @param <U> type of the first element
 * @param <V> type of the second element
 */
public class Pair<U, V> extends Storage {
    public final U first;
    public final V second;

    public Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public @View String toString() {
        return StringSupport.concat("(", first, ", ", second, ")");
    }

    public static <U, V> Pair <U, V> of(U a, V b) {
        return new Pair<>(a, b);
    }
}
