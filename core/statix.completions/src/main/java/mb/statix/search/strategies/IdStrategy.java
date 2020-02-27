package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;

import java.util.stream.Stream;


/**
 * The id() strategy, which always succeeds.
 */
public final class IdStrategy<T, CTX> implements Strategy<T, T, CTX> {

    @Override
    public Stream<T> apply(CTX ctx, T input) throws InterruptedException {
        return Stream.of(input);
    }

    @Override
    public String toString() {
        return "id";
    }

}
