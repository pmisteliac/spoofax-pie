package mb.statix.search.strategies;

import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.strategies.Strategy;
import mb.statix.search.*;
import mb.statix.solver.IConstraint;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Focus on a single constraint.
 */
public final class FocusStrategy<C extends IConstraint> implements Strategy<SolverState, FocusedSolverState<C>, SolverContext> {

    private final Class<C> constraintClass;
    private final Predicate<C> predicate;

    /**
     * Initializes a new instance of the {@link FocusStrategy} class.
     *
     * @param constraintClass the class of constraints that can be focused on
     * @param predicate the predicate that determine which constraints to focus on
     */
    public FocusStrategy(Class<C> constraintClass, Predicate<C> predicate) {
        this.constraintClass = constraintClass;
        this.predicate = predicate;
    }

    @Override
    public Stream<FocusedSolverState<C>> apply(SolverContext ctx, SolverState input) throws InterruptedException {
        //noinspection unchecked
        Optional<C> focus = input.getConstraints().stream()
                .filter(c -> constraintClass.isAssignableFrom(c.getClass()))
                .map(c -> (C)c)
                .filter(predicate)
                .findFirst();
        return focus.map(c -> Stream.of(new FocusedSolverState<>(input, c))).orElseGet(Stream::empty);

    }

    @Override
    public String toString() {
        return "focus(" + constraintClass.getSimpleName() + ")";
    }

}
