package mb.statix.search.strategies;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.strategies.Strategy;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CResolveQuery;
import mb.statix.generator.scopegraph.DataWF;
import mb.statix.generator.scopegraph.Env;
import mb.statix.generator.scopegraph.Match;
import mb.statix.generator.scopegraph.NameResolution;
import mb.statix.generator.strategy.ResolveDataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.search.*;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;
import mb.statix.spoofax.StatixTerms;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;


/**
 * Expands the selected query.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ExpandQueryStrategy implements Strategy<FocusedSolverState<CResolveQuery>, SolverState, SolverContext> {

    @Override
    public Stream<SolverState> apply(SolverContext ctx, FocusedSolverState<CResolveQuery> input) throws InterruptedException {
        final CResolveQuery query = input.getFocus();

        final IState.Immutable state = input.getInnerState().getState();
        final IUniDisunifier unifier = state.unifier();

        // Find the scope
        final Scope scope = Scope.matcher().match(query.scopeTerm(), unifier).orElse(null);
        if(scope == null) throw new IllegalArgumentException("cannot resolve query: no scope");

        // Determine data equivalence (either: true, false, or null when it could not be determined)
        final Boolean isAlways;
        try {
            isAlways = query.min().getDataEquiv().isAlways(ctx.getSpec()).orElse(null);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(isAlways == null) {
            throw new IllegalArgumentException("cannot resolve query: cannot decide data equivalence");
        }

        final ICompleteness.Immutable completeness = input.getInnerState().getCompleteness();
        final Predicate2<Scope, ITerm> isComplete2 = (s, l) -> completeness.isComplete(s, l, state.unifier());
        final LabelWF<ITerm> labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
        final LabelOrder<ITerm> labelOrd = new RelationLabelOrder(query.min().getLabelOrder());
        final DataWF<ITerm, CEqual> dataWF =
                new ResolveDataWF(state, completeness, query.filter().getDataWF(), query);

        // @formatter:off
        final NameResolution<Scope, ITerm, ITerm, CEqual> nameResolution = new NameResolution<>(
                ctx.getSpec(),
                state.scopeGraph(), query.relation(),
                labelWF, labelOrd, isComplete2,
                dataWF, isAlways, isComplete2);
        // @formatter:on

        // Find all possible declarations the resolution could resolve to.
        final AtomicInteger count = new AtomicInteger(1);
        try {
            nameResolution.resolve(scope, () -> {
                count.incrementAndGet();
                return false;
            });
        } catch(ResolutionException e) {
            throw new IllegalArgumentException("cannot resolve query: delayed on " + e.getMessage());
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        // For each declaration:
        return IntStream.range(0, count.get()).mapToObj(idx -> {
            final AtomicInteger select = new AtomicInteger(idx);
            final Env<Scope, ITerm, ITerm, CEqual> env;
            try {
                env = nameResolution.resolve(scope, () -> select.getAndDecrement() == 0);
            } catch(ResolutionException e) {
                throw new IllegalArgumentException("cannot resolve query: delayed on " + e.getMessage());
            } catch(InterruptedException e) {
                // Unfortunate that we have to do this
                throw new RuntimeException(e);
            }

            // Unconditional matches
            final List<Match<Scope, ITerm, ITerm, CEqual>> reqMatches =
                    env.matches.stream().filter(m -> !m.condition.isPresent()).collect(Collectors.toList());
            // Conditional matches
            final List<Match<Scope, ITerm, ITerm, CEqual>> optMatches =
                    env.matches.stream().filter(m -> m.condition.isPresent()).collect(Collectors.toList());

            if (env.matches.isEmpty()) {
                return Stream.<SolverState>empty();
            }

            // Determine the range of sizes the query result set can be
            final Range<Integer> resultSize = resultSize(query.resultTerm(), unifier, env.matches.size());

            // For each possible size:
            return IntStream.rangeClosed(resultSize.lowerEndpoint(), resultSize.upperEndpoint())
                .mapToObj(size -> StreamUtils.subsetsOfSize(optMatches.stream(), size).map(matches -> {
                    List<Match<Scope, ITerm, ITerm, CEqual>> rejects =
                            optMatches.stream().filter(m -> !matches.contains(m)).collect(Collectors.toList());

                    final Env.Builder<Scope, ITerm, ITerm, CEqual> subEnvBuilder = Env.builder();
                    reqMatches.forEach(subEnvBuilder::match);
                    matches.forEach(subEnvBuilder::match);
                    rejects.forEach(subEnvBuilder::reject);
                    env.rejects.forEach(subEnvBuilder::reject);
                    final Env<Scope, ITerm, ITerm, CEqual> subEnv = subEnvBuilder.build();

                    final List<ITerm> pathTerms = subEnv.matches.stream().map(m -> StatixTerms.explicate(m.path))
                            .collect(ImmutableList.toImmutableList());
                    final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
                    constraints.add(new CEqual(B.newList(pathTerms), query.resultTerm(), query));
                    subEnv.matches.stream().flatMap(m -> Optionals.stream(m.condition)).forEach(constraints::add);
                    subEnv.rejects.stream().flatMap(m -> Optionals.stream(m.condition)).forEach(condition -> {
                        constraints.add(new CInequal(ImmutableSet.of(), condition.term1(), condition.term2(),
                                condition.cause().orElse(null), condition.message().orElse(null)));
                    });
                    return input.getInnerState().updateConstraints(constraints.build(), Iterables2.singleton(query));
                })).flatMap(stream -> stream);
        }).flatMap(stream -> stream);
    }

    /**
     * Determine the possible sizes of the result sets of the query.
     *
     * Many queries expect to resolve to a set with a single declaration,
     * but this is not necessarily so. This method returns the possible
     * sizes of the query result set as a range. The minimum is 0 and the
     * maximum is the number of declarations found.
     *
     * We distinguish these cases:<ul>
     * <li>empty list [] -> [0]
     * <li>fixed-size list [a, b, c] -> [3]
     * <li>variable-length list [a, b | xs] -> [2, declarationCount]
     * <li>something else -> [0, declarationCount]
     * </ul>
     * For example, if the result term is a list [a, b, c] then we return a singleton range [3].
     * If the result term is a list that has a variable for a tail, [a, b | xs] then we return a range [2, max].
     * If the result term is just
     *
     * @param result the result term of the query,
     *               from which we try to deduce if the query expects a singleton set or not
     * @param unifier the unifier
     * @param declarationCount the number of declarations found
     * @return the range of possible query result set sizes
     */
    private Range<Integer> resultSize(ITerm result, IUniDisunifier unifier, int declarationCount) {
        // @formatter:off
        final AtomicInteger min = new AtomicInteger(0);
        return M.<Range<Integer>>list(ListTerms.<Range<Integer>>casesFix(
                (m, cons) -> {
                    // Increment the minimum
                    min.incrementAndGet();
                    return m.apply((IListTerm) unifier.findTerm(cons.getTail()));
                },
                (m, nil) -> Range.singleton(min.get()),
                (m, var) -> Range.closed(min.get(), declarationCount)
        )).match(result, unifier).orElse(Range.closed(0, declarationCount));
        // @formatter:on
    }

    @Override
    public String toString() {
        return "expandQuery";
    }

}
