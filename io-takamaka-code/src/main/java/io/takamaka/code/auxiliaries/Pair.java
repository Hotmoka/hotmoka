package io.takamaka.code.auxiliaries;

import io.takamaka.code.lang.Storage;
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;
        if (!first.equals(pair.first))
            return false;
        return second.equals(pair.second);
    }

    @Override
    public @View int hashCode() {
        return first.hashCode() ^ second.hashCode();
    }

    @Override
    public @View String toString() {
        return "(" + first + ", " + second + ")";
    }

    public static <U, V> Pair <U, V> of(U a, V b) {
        return new Pair<>(a, b);
    }
}
