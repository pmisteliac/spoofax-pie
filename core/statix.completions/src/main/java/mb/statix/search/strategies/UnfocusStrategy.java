package mb.statix.search.strategies;

import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.strategies.Strategy;
import mb.statix.search.FocusedSolverState;
import mb.statix.solver.IConstraint;

import java.util.stream.Stream;


/**
 * Unfocuses any constraint.
 */
public final class UnfocusStrategy<C extends IConstraint> implements Strategy<FocusedSolverState<C>, SolverState, SolverContext> {

    @Override
    public Stream<SolverState> apply(SolverContext ctx, FocusedSolverState<C> input) throws InterruptedException {
        return Stream.of(input.getInnerState());

    }

    @Override
    public String toString() {
        return "unfocus";
    }

}
