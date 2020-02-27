package mb.statix.common.strategies;

import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;

import java.util.stream.Stream;


/**
 * Performs inference on the search state.
 *
 * NOTE: Call the isSuccessful() strategy on this result to ensure it has no errors.
 */
public final class InferStrategy implements Strategy<SolverState, SolverState, SolverContext> {

    @Override
    public Stream<SolverState> apply(SolverContext ctx, SolverState state) throws InterruptedException {

        final SolverResult result = Solver.solve(
                ctx.getSpec(),
                state.getState(),
                state.getConstraints(),
                state.getDelays(),
                state.getCompleteness(),
                new NullDebugContext()
        );

        // NOTE: Call the isSuccessful() strategy on this result to ensure it has no errors.

        return Stream.of(SolverState.fromSolverResult(result, state.getExistentials()));
    }

    @Override
    public String toString() {
        return "infer";
    }

}
