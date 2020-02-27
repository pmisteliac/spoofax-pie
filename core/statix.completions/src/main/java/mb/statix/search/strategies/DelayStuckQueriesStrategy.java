package mb.statix.search.strategies;

import com.google.common.collect.Maps;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.strategies.Strategy;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CResolveQuery;
import mb.statix.generator.scopegraph.DataWF;
import mb.statix.generator.scopegraph.NameResolution;
import mb.statix.generator.strategy.ResolveDataWF;
import mb.statix.scopegraph.reference.*;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.search.*;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;
import mb.statix.spec.Spec;
import org.metaborg.util.functions.Predicate2;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * Delays stuck queries.
 */
public final class DelayStuckQueriesStrategy implements Strategy<SolverState, SolverState, SolverContext> {

    @Override
    public Stream<SolverState> apply(SolverContext ctx, SolverState input) throws InterruptedException {
        final IState.Immutable state = input.getState();
        final ICompleteness.Immutable completeness = input.getCompleteness();

        final java.util.Map<IConstraint, Delay> delays = Maps.newHashMap();
        input.getConstraints().stream().filter(c -> c instanceof CResolveQuery).map(c -> (CResolveQuery) c).forEach(q -> checkDelay(ctx.getSpec(), q, state, completeness).ifPresent(d -> {
            delays.put(q, d);
        }));

        return Stream.of(input.delay(delays.entrySet()));
    }

    private Optional<Delay> checkDelay(Spec spec, CResolveQuery query, IState.Immutable state, ICompleteness.Immutable completeness) {
        final IUniDisunifier unifier = state.unifier();

        if(!unifier.isGround(query.scopeTerm())) {
            return Optional.of(Delay.ofVars(unifier.getVars(query.scopeTerm())));
        }
        final Scope scope = Scope.matcher().match(query.scopeTerm(), unifier).orElse(null);
        if(scope == null) {
            return Optional.empty();
        }

        final Boolean isAlways;
        try {
            isAlways = query.min().getDataEquiv().isAlways(spec).orElse(null);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(isAlways == null) {
            return Optional.empty();
        }

        final Predicate2<Scope, ITerm> isComplete2 = (s, l) -> completeness.isComplete(s, l, state.unifier());
        final LabelWF<ITerm> labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
        final LabelOrder<ITerm> labelOrd = new RelationLabelOrder(query.min().getLabelOrder());
        final DataWF<ITerm, CEqual> dataWF = new ResolveDataWF( state, completeness, query.filter().getDataWF(), query);

        // @formatter:off
        final NameResolution<Scope, ITerm, ITerm, CEqual> nameResolution = new NameResolution<>(
                spec,
                state.scopeGraph(), query.relation(),
                labelWF, labelOrd, isComplete2,
                dataWF, isAlways, isComplete2);
        // @formatter:on

        try {
            nameResolution.resolve(scope, () -> false);
        } catch(IncompleteDataException e) {
            return Optional.of(Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation())));
        } catch(IncompleteEdgeException e) {
            return Optional.of(Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label())));
        } catch(ResolutionException e) {
            throw new RuntimeException("Unexpected resolution exception.", e);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return "delayStuckQueries";
    }

}
