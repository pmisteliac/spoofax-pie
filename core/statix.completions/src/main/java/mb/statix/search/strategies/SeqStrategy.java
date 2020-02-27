package mb.statix.search.strategies;

import com.google.common.collect.ImmutableList;
import mb.statix.common.strategies.Strategy;
import mb.statix.search.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The s1; s2; ...; sn strategy.
 */
public final class SeqStrategy<I, O, CTX> implements Strategy<I, O, CTX> {

    private final List<Strategy<?, ?, CTX>> strategies;

    private SeqStrategy(List<Strategy<?, ?, CTX>> strategies) {
        this.strategies = strategies;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Stream<O> apply(CTX ctx, I input) throws InterruptedException {
        Stream<Object> values = Stream.of(input);
        for (Strategy strategy : this.strategies) {
            values = values.flatMap(v -> {
                try {
                    return strategy.apply(ctx, v);
                } catch (InterruptedException e) {
                    // Very annoying that we have to do this
                    throw new RuntimeException(e);
                }
            });
        }
        return (Stream<O>)values;
    }

    @Override
    public String toString() {
        return this.strategies.stream()
                .map(Object::toString)
                .collect(Collectors.joining("; ", "(", ")"));
    }

    /**
     * A builder for sequences of strategies.
     *
     * @param <I> the input type
     * @param <O> the output type
     */
    public static class Builder<I, O, CTX> {

        private final ImmutableList.Builder<Strategy<?, ?, CTX>> ss = ImmutableList.builder();

        public Builder(Strategy<I, O, CTX> s) {
            ss.add(s);
        }

        public <X> Builder<I, X, CTX> $(Strategy<O, X, CTX> s) {
            ss.add(s);
            //noinspection unchecked
            return (Builder<I, X, CTX>) this;
        }

        public SeqStrategy<I, O, CTX> $() {
            return new SeqStrategy<>(ss.build());
        }

    }

}
