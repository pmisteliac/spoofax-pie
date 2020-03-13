package mb.statix.common;

import com.google.common.collect.ImmutableList;
import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.util.CapsuleUtil;
import mb.resource.ResourceKey;
import mb.statix.common.strategies.InferStrategy;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CUser;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.ASolverResult;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Statix-based semantic analyzer.
 */
public class StatixAnalyzer {

    private final Logger log;
    private final StrategoTerms strategoTerms;
    private final ITermFactory termFactory;
    private final StatixSpec spec;

    @Inject public StatixAnalyzer(
        StatixSpec spec,
        ITermFactory termFactory,
        LoggerFactory loggerFactory
    ) {
        this.spec = spec;
        this.termFactory = termFactory;
        this.strategoTerms = new StrategoTerms(termFactory);
        this.log = loggerFactory.create(getClass());
    }

    /**
     * Creates a new solver context.
     *
     * @return the solver context
     */
    public SolverContext createContext() {
        return new SolverContext(spec.getSpec());
    }

    /**
     * Analyzes the specified AST in the specified solver context.
     *
     * @param ctx the solver context
     * @param statixAst the AST to analyze
     * @return the resulting solver state
     * @throws InterruptedException
     */
    public SolverState analyze(SolverContext ctx, ITerm statixAst) throws InterruptedException {
        IConstraint rootConstraint = getRootConstraint(statixAst, "static-semantics");   /* TODO: Get the spec name from the spec? */
        log.info("Analyzing: " + rootConstraint);
        return analyze(ctx, spec.getSpec(), rootConstraint);
    }

    /**
     * Gets the root constraint of the specification.
     *
     * @return the root constraint
     */
    private IConstraint getRootConstraint(ITerm statixAst, String specName) {
        String rootRuleName = "programOK";      // FIXME: Ability to specify root rule somewhere
        String qualifiedName = makeQualifiedName(specName, rootRuleName);
        return new CUser(qualifiedName, Collections.singletonList(statixAst), null);
    }

    /**
     * Returns the qualified name of the rule.
     *
     * @param specName the name of the specification
     * @param ruleName the name of the rule
     * @return the qualified name of the rule, in the form of {@code <specName>!<ruleName>}.
     */
    private String makeQualifiedName(String specName, String ruleName) {
        if (specName.equals("") || ruleName.contains("!")) return ruleName;
        return specName + "!" + ruleName;
    }

    /**
     * Invokes analysis.
     *
     * @param spec the Statix specification
     * @param rootConstraint the root constraint
     * @return the resulting analysis result
     */
    private SolverState analyze(SolverContext ctx, Spec spec, IConstraint rootConstraint) throws InterruptedException {
        SolverState startState = SolverState.of(spec, State.of(spec), ImmutableList.of(rootConstraint));
        return new InferStrategy().apply(ctx, startState).findFirst().orElseThrow(() -> new IllegalStateException("This cannot be happening."));
    }

}
