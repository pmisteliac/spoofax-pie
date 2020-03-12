package mb.tiger;

import com.google.common.collect.ImmutableList;
import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.matching.TermMatch;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.resource.ResourceKey;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.StatixAnalyzer;
import mb.statix.common.StatixSpec;
import mb.statix.common.strategies.InferStrategy;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CUser;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.strc.$Apply$Closure_0_0;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mb.nabl2.terms.matching.TermMatch.M;

/**
 * Statix-based semantic analyzer for Tiger.
 */
public class TigerAnalyzer extends StatixAnalyzer {

    @Inject public TigerAnalyzer(
        StatixSpec spec,
        ITermFactory termFactory,
        LoggerFactory loggerFactory
    ) {
        super(spec, termFactory, loggerFactory);
    }
//
//    /**
//     * Creates a new solver context.
//     *
//     * @return the solver context
//     */
//    public SolverContext createContext() {
//        return new SolverContext(spec.spec);
//    }
//
//    /**
//     * Analyzes the specified AST in the specified solver context.
//     *
//     * @param ctx the solver context
//     * @param statixAst the AST to analyze
//     * @param trackedVars the term variables that we're interested in
//     * @return the resulting solver state
//     * @throws InterruptedException
//     */
//    public SolverState analyze(SolverContext ctx, ITerm statixAst, List<ITermVar> trackedVars) throws InterruptedException {
//        IConstraint rootConstraint = getRootConstraint(statixAst, "static-semantics", trackedVars);   /* TODO: Get the spec name from the spec? */
//        log.info("Analyzing: " + rootConstraint);
//        return analyze(ctx, spec.spec, rootConstraint);
//    }
//
//    /**
//     * Gets the root constraint of the specification.
//     *
//     * @return the root constraint
//     */
//    private IConstraint getRootConstraint(ITerm statixAst, String specName, List<ITermVar> trackedVars) {
//        String rootRuleName = "programOK";      // FIXME: Ability to specify root rule somewhere
//        String qualifiedName = makeQualifiedName(specName, rootRuleName);
//        // FIXME: It is annoying to have to specify the placeholder we're interested in as an existential.
//        return new CExists(trackedVars, new CUser(qualifiedName, Collections.singletonList(statixAst), null));
//    }
//
//    /**
//     * Returns the qualified name of the rule.
//     *
//     * @param specName the name of the specification
//     * @param ruleName the name of the rule
//     * @return the qualified name of the rule, in the form of {@code <specName>!<ruleName>}.
//     */
//    private String makeQualifiedName(String specName, String ruleName) {
//        if (specName.equals("") || ruleName.contains("!")) return ruleName;
//        return specName + "!" + ruleName;
//    }
//
//    /**
//     * Invokes analysis.
//     *
//     * @param spec the Statix specification
//     * @param rootConstraint the root constraint
//     * @return the resulting analysis result
//     */
//    private SolverState analyze(SolverContext ctx, Spec spec, IConstraint rootConstraint) throws InterruptedException {
//        SolverState startState = SolverState.of(spec, State.of(spec), ImmutableList.of(rootConstraint));
//        return new InferStrategy().apply(ctx, startState).findFirst().orElseThrow(() -> new IllegalStateException("This cannot be happening."));
//    }
}
