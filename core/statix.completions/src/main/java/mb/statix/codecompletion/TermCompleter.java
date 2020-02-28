package mb.statix.codecompletion;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.strategies.Strategy;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.search.strategies.Strategies;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.search.strategies.SearchStrategies.*;
import static mb.statix.search.strategies.Strategies.*;


/**
 * The term completer.
 */
public final class TermCompleter {

    private static Strategy<SolverState, SolverState, SolverContext> completionStrategy =
    // @formatter:off
        seq(print(Strategies.<SolverState, SolverContext>id()))
         .$(seq(debug(limit(1, focus(CUser.class)), s -> System.out.println("Focused on: " + s)))
             .$(expandRule())
             .$(infer())
             .$(isSuccessful())
             .$(delayStuckQueries())
             .$())
         .$(repeat(seq(limit(1, focus(CResolveQuery.class)))
            .$(expandQuery())
            .$(infer())
            .$(isSuccessful())
            .$(delayStuckQueries())
            .$()
         ))
         .$(print())
         .$();
    // @formatter:on

    /**
     * Completes the specified constraint.
     *
     * @param ctx the search context
     * @param state the initial search state
     * @param placeholderVar the var of the placeholder to complete
     * @return the resulting completion proposals
     */
    public List<ITerm> complete(SolverContext ctx, SolverState state, ITermVar placeholderVar) throws InterruptedException {
        return completeNodes(ctx, state).map(s -> project(placeholderVar, s)).collect(Collectors.toList());
    }

    /**
     * Completes the specified constraint.
     *
     * @param ctx the search context
     * @param state the initial search state
     * @return the resulting states
     */
    public Stream<SolverState> completeNodes(SolverContext ctx, SolverState state) throws InterruptedException {
        return completionStrategy.apply(ctx, state);
    }

    private static ITerm project(ITermVar placeholderVar, SolverState s) {
        if(s.getExistentials() != null && s.getExistentials().containsKey(placeholderVar)) {
            return s.getState().unifier().findRecursive(s.getExistentials().get(placeholderVar));
        } else {
            return placeholderVar;
        }
    }
}
