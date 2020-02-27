package mb.statix.search.strategies;

import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.strategies.Strategy;

import java.util.stream.Stream;


/**
 * Search strategy that only succeeds if the search state has no errors.
 */
public final class IsSuccessfulStrategy implements Strategy<SolverState, SolverState, SolverContext> {

    @Override
    public Stream<SolverState> apply(SolverContext ctx, SolverState state) throws InterruptedException {
        if (state.hasErrors()) {
            return Stream.empty();
        } else {
            return Stream.of(state);
        }
    }

    @Override
    public String toString() {
        return "isSuccessful";
    }

}
