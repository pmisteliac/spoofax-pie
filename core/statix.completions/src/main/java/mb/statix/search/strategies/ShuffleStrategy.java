package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;
import mb.statix.search.RandomUtils;
import mb.statix.search.StreamUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;


/**
 * The shuffle(rng, s) strategy, which applies s, then shuffles the results.
 */
public final class ShuffleStrategy<A, B, CTX> implements Strategy<A, B, CTX> {

    private final Random rng;
    private final Strategy<A, B, CTX> strategy;

    public ShuffleStrategy(Random rng, Strategy<A, B, CTX> strategy) {
        this.rng = rng;
        this.strategy = strategy;
    }

    @Override
    public Stream<B> apply(CTX ctx, A input) throws InterruptedException {
        return StreamUtils.transform(this.strategy.apply(ctx, input), l -> {
            List<B> list = new ArrayList<>(l);
            Collections.shuffle(list, this.rng);
            return list;
        });
    }

    @Override
    public String toString() {
        return "shuffle(" + RandomUtils.getSeed(rng) + ", " + strategy.toString() + ")";
    }

}
