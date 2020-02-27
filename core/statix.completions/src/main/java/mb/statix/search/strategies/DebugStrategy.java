package mb.statix.search.strategies;

import mb.statix.common.strategies.Strategy;
import mb.statix.search.StreamUtils;

import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * The debug(s, a) strategy wraps a strategy, evaluates it, and applies an action to each element.
 */
public final class DebugStrategy<I, O, CTX> implements Strategy<I, O, CTX> {

    private final Consumer<O> action;
    private final Strategy<I, O, CTX> strategy;

    public DebugStrategy(Strategy<I, O, CTX> strategy, Consumer<O> action) {
        this.strategy = strategy;
        this.action = action;
    }

    @Override
    public Stream<O> apply(CTX ctx, I input) throws InterruptedException {
        // This buffers the entire stream.
        // This has a performance implication, but is required for a better debugging experience.
        return StreamUtils.transform(this.strategy.apply(ctx, input), c -> { c.forEach(this.action); return c; });
    }

    @Override
    public String toString() {
        return "debug(" + strategy.toString() + ")";
    }

}
