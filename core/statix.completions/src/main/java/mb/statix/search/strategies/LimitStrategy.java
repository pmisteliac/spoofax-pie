package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;

import java.util.stream.Stream;


/**
 * The limit(i, s) strategy, which applies s, then returns only the i first results.
 */
public final class LimitStrategy<A, B, CTX> implements Strategy<A, B, CTX> {

    private final int limit;
    private final Strategy<A, B, CTX> strategy;

    public LimitStrategy(int limit, Strategy<A, B, CTX> strategy) {
        this.limit = limit;
        this.strategy = strategy;
    }

    @Override
    public Stream<B> apply(CTX ctx, A input) throws InterruptedException {
        return this.strategy.apply(ctx, input).limit(limit);
    }

    @Override
    public String toString() {
        return "limit(" + limit + ", " + strategy.toString() + ")";
    }

}
