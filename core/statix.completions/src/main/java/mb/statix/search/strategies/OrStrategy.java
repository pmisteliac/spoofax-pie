package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;
import mb.statix.search.*;
import one.util.streamex.StreamEx;

import java.util.stream.Stream;


/**
 * The non-deterministic or() strategy, which splits the search tree.
 */
public final class OrStrategy<I, O, CTX> implements Strategy<I, O, CTX> {

    private final Strategy<I, O, CTX> strategy1;
    private final Strategy<I, O, CTX> strategy2;

    public OrStrategy(Strategy<I, O, CTX> strategy1, Strategy<I, O, CTX> strategy2) {
        this.strategy1 = strategy1;
        this.strategy2 = strategy2;
    }

    @Override
    public Stream<O> apply(CTX ctx, I input) throws InterruptedException {
        return StreamEx.of(this.strategy1.apply(ctx, input)).append(this.strategy2.apply(ctx, input));
    }

    @Override
    public String toString() {
        return strategy1.toString() + " + " + strategy2.toString();
    }

}
