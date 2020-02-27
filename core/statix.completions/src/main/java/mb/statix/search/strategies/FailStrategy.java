package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;
import mb.statix.search.*;

import java.util.stream.Stream;


/**
 * The fail() strategy, which always fails.
 */
public final class FailStrategy<T, CTX> implements Strategy<T, T, CTX> {

    @Override
    public Stream<T> apply(CTX ctx, T input) throws InterruptedException {
        return Stream.empty();
    }

    @Override
    public String toString() {
        return "fail";
    }

}
