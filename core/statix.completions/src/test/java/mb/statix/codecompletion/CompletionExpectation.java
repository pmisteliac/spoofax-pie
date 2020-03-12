package mb.statix.codecompletion;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermPattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.statix.common.SolverState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An incomplete AST,
 * and a mapping from term variables to their expected ASTs.
 */
@Value.Immutable(builder = false)
abstract class CompletionExpectation<T extends ITerm> {

    /**
     * Gets the AST that is being built.
     *
     * @return the incomplete AST
     */
    @Value.Parameter public abstract T getIncompleteAst();

    /**
     * Gets the expected values for the various placeholders in the term.
     *
     * @return the expectations
     */
    @Value.Parameter public abstract Map<ITermVar, ITerm> getExpectations();

    /**
     * Gets the solver state of the incomplete AST.
     *
     * @return the solver state
     */
    @Value.Parameter public abstract @Nullable SolverState getState();

    /**
     * Gets the set of term variables for which we need to find completions.
     *
     * @return the set of term variables; or an empty set when completion is done
     */
    @Value.Derived public Set<ITermVar> getVars() {
        return getExpectations().keySet();
    }

    /**
     * Whether the AST is complete.
     *
     * @return {@code true} when the AST is complete; otherwise, {@code false}
     */
    @Value.Derived public boolean isComplete() {
        return getVars().isEmpty();
    }

    /**
     * Replaces the specified term variable with the specified term,
     * if it is the term that we expected.
     *
     * @param var the term variable to replace
     * @param proposal the proposal to replace it with, which may contain term variables
     * @return the resulting incomplete AST if replacement succeeded; otherwise, {@code null} when it doesn't fit
     */
    public @Nullable ImmutableCompletionExpectation<? extends ITerm> tryReplace(ITermVar var, TermCompleter.CompletionSolverProposal proposal) {
        ITerm term = proposal.getTerm();
        if (var.equals(term)) {
            // Trying to replace by the same variable is not allowed
            return null;
        }
        ITerm expectedTerm = getExpectations().get(var);
        // Does the term we got, including variables, match the expected term?
        Optional<ISubstitution.Immutable> optSubstitution = TermPattern.P.fromTerm(term).match(expectedTerm);
        if (!optSubstitution.isPresent()) return null;
        // Yes, and the substitution shows the new variables and their expected term values
        ISubstitution.Immutable substitution = optSubstitution.get();
        HashMap<ITermVar, ITerm> expectedAsts = new HashMap<>();
        for(Map.Entry<ITermVar, ITerm> entry : substitution.entrySet()) {
            expectedAsts.put(entry.getKey(), entry.getValue());
        }
        ITerm newIncompleteAst = PersistentSubstitution.Immutable.of(var, term).apply(getIncompleteAst());
        return ImmutableCompletionExpectation.of(newIncompleteAst, expectedAsts, proposal.getNewState());
    }
}
