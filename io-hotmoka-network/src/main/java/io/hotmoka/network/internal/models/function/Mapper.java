package io.hotmoka.network.internal.models.function;

/**
 * Represents a function that accepts one argument and produces a result.
 * This is a functional interface whose functional method is map(Object)
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface Mapper<T, R> {

    /**
     * It returns the mapping of T
     * @param input the input
     * @return the mapping of T into R
     * @throws Exception is some exception is raised inside the implementation method
     */
    R map(T input) throws Exception;
}
