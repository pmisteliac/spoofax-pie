package mb.statix.search.strategies;

import mb.statix.common.strategies.InferStrategy;
import mb.statix.solver.IConstraint;

import java.util.function.Predicate;


/**
 * Convenience functions for creating search strategies.
 */
public final class SearchStrategies {

    /**
     * Delays stuck queries in the search state.
     *
     * @return the resulting strategy
     */
    public static DelayStuckQueriesStrategy delayStuckQueries() {
        return new DelayStuckQueriesStrategy();
    }

    /**
     * Expands queries in the search state.
     *
     * @return the resulting strategy
     */
    public static ExpandQueryStrategy expandQuery() {
        return new ExpandQueryStrategy();
    }

    /**
     * Expands rules in the search state.
     *
     * @return the resulting strategy
     */
    public static ExpandRuleStrategy expandRule() {
        return new ExpandRuleStrategy();
    }

    /**
     * Focuses the search state on a particular constraint.
     *
     * @param constraintClass the class of constraints to focus on
     * @param predicate the predicate indicating which constraint to focus on
     * @param <C> the type of constraints to focus on
     * @return the resulting strategy
     */
    public static <C extends IConstraint> FocusStrategy<C> focus(Class<C> constraintClass, Predicate<C> predicate) {
        return new FocusStrategy<>(constraintClass, predicate);
    }

    /**
     * Focuses the search state on a particular constraint, unconditionally.
     *
     * @param constraintClass the class of constraints to focus on
     * @param <C> the type of constraints to focus on
     * @return the resulting strategy
     */
    public static <C extends IConstraint> FocusStrategy<C> focus(Class<C> constraintClass) {
        return focus(constraintClass, c -> true);
    }

    /**
     * Performs inference on the search strategy.
     *
     * @return the resulting strategy
     */
    public static InferStrategy infer() {
        return new InferStrategy();
    }

    /**
     * Search strategy that only succeeds if the search state has no errors.
     *
     * @return the resulting strategy
     */
    public static IsSuccessfulStrategy isSuccessful() { return new IsSuccessfulStrategy(); }

    /**
     * Unfocuses.
     *
     * @return the resulting strategy
     */
    public static <C extends IConstraint> UnfocusStrategy<C> unfocus() {
        return new UnfocusStrategy<>();
    }
}
