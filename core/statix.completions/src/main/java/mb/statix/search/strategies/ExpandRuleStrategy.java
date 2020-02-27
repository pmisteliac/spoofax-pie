package mb.statix.search.strategies;

import com.google.common.collect.ImmutableSet;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.strategies.Strategy;
import mb.statix.constraints.CUser;
import mb.statix.search.*;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;

import java.util.stream.Stream;


/**
 * Expands the selected rule.
 */
public final class ExpandRuleStrategy implements Strategy<FocusedSolverState<CUser>, SolverState, SolverContext> {

    @Override
    public Stream<SolverState> apply(SolverContext ctx, FocusedSolverState<CUser> state) throws InterruptedException {
        CUser focus = state.getFocus();

        final ImmutableSet<Rule> rules = ctx.getSpec().rules().getOrderIndependentRules(focus.name());
        SolverState searchState = state.getInnerState();
        return RuleUtil.applyAll(searchState.getState(), rules, focus.args(), focus).stream()
                .map(t -> searchState.withApplyResult(t._2(), focus));
    }

    @Override
    public String toString() {
        return "expandRule";
    }

}
