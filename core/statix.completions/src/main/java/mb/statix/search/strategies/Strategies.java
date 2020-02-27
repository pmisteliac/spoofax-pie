package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;
import mb.statix.search.*;

import java.util.Random;
import java.util.function.Consumer;


/**
 * Convenience functions for creating strategies.
 */
public final class Strategies {

    /**
     * Performs an action on all results of the given strategy.
     *
     * @param s the strategy
     * @param action the action to perform
     * @param <T> the type of input for the strategy
     * @param <R> the type of outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, R, CTX> DebugStrategy<T, R, CTX> debug(Strategy<T, R, CTX> s, Consumer<R> action) {
        return new DebugStrategy<>(s, action);
    }

    /**
     * Always fails.
     *
     * @param <T> the type of inputs and outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, CTX> FailStrategy<T, CTX> fail() {
        return new FailStrategy<>();
    }

    /**
     * Identity strategy.
     *
     * @param <T> the type of input and outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, CTX> IdStrategy<T, CTX> id() {
        return new IdStrategy<>();
    }

    /**
     * Limits the number of results of the given search strategy.
     *
     * @param limit the maximum number of results
     * @param s the strategy
     * @param <T> the type of input for the strategy
     * @param <R> the type of outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, R, CTX> LimitStrategy<T, R, CTX> limit(int limit, Strategy<T, R, CTX> s) {
        return new LimitStrategy<>(limit, s);
    }

    /**
     * Applies two strategies non-deterministically.
     *
     * @param s1 the first strategy
     * @param s2 the second strategy
     * @param <T> the type of input for the strategy
     * @param <R> the type of outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, R, CTX> OrStrategy<T, R, CTX> or(Strategy<T, R, CTX> s1, Strategy<T, R, CTX> s2) {
        return new OrStrategy<>(s1, s2);
    }

    /**
     * Prints all values resulting from the given strategy.
     *
     * @param s the strategy
     * @param <T> the type of input for the strategy
     * @param <R> the type of outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, R, CTX> DebugStrategy<T, R, CTX> print(Strategy<T, R, CTX> s) {
        return new DebugStrategy<>(s, v -> System.out.println(v.toString()));
    }

    /**
     * Prints the value.
     *
     * @param <T> the type of input and outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, CTX> DebugStrategy<T, T, CTX> print() {
        return print(id());
    }

    /**
     * Repeatedly applies a strategy to the results until the strategy fails.
     *
     * @param s the strategy
     * @param <T> the type of input and outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, CTX> RepeatStrategy<T, CTX> repeat(Strategy<T, T, CTX> s) {
        return new RepeatStrategy<>(s);
    }

    /**
     * Starts a sequence of strategies to apply.
     *
     * @param s the first strategy to apply
     * @param <T> the type of input for the strategy
     * @param <R> the type of outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, R, CTX> SeqStrategy.Builder<T, R, CTX> seq(Strategy<T, R, CTX> s) {
        return new SeqStrategy.Builder<>(s);
    }

    /**
     * Shuffles the results of the given strategy.
     *
     * @param rng the random number generator to use
     * @param s the strategy
     * @param <T> the type of input for the strategy
     * @param <R> the type of outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, R, CTX> ShuffleStrategy<T, R, CTX> shuffle(Random rng, Strategy<T, R, CTX> s) {
        return new ShuffleStrategy<>(rng, s);
    }

    /**
     * Shuffles the results of the given strategy with a new random number generator.
     *
     * @param s the strategy
     * @param <T> the type of input for the strategy
     * @param <R> the type of outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, R, CTX> ShuffleStrategy<T, R, CTX> shuffle(Strategy<T, R, CTX> s) {
        return shuffle(new Random(), s);
    }

    /**
     * Attempts to apply the given strategy on the input.
     *
     * @param s the strategy
     * @param <T> the type of input and outputs for the strategy
     * @param <CTX> the context of the strategy
     * @return the resulting strategy
     */
    public static <T, CTX> TryStrategy<T, CTX> try_(Strategy<T, T, CTX> s) {
        return new TryStrategy<>(s);
    }

}
