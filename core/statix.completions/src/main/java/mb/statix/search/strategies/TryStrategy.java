package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;
import one.util.streamex.StreamEx;

import java.util.stream.Stream;


/**
 * The try(s) strategy, which applies s but always succeeds.
 */
public final class TryStrategy<T, CTX> implements Strategy<T, T, CTX> {

    private final Strategy<T, T, CTX> s;

    public TryStrategy(Strategy<T, T, CTX> s) {
        this.s = s;
    }

    @Override
    public Stream<T> apply(CTX ctx, T input) throws InterruptedException {
        return StreamEx.of(this.s.apply(ctx, input)).ifEmpty(input);
    }

    @Override
    public String toString() {
        return "try(" + s.toString() + ")";
    }

}
