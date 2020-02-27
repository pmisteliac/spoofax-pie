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
import mb.statix.common.strategies.InferStrategy;
import mb.statix.constraints.CUser;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.strc.$Apply$Closure_0_0;

import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mb.nabl2.terms.matching.TermMatch.M;

public class TigerAnalyzer {

    private final TigerStatixSpec spec;
    private final ITermFactory termFactory;
    private final StrategoTerms strategoTerms;
    private final Logger log;

    public TigerAnalyzer(
        TigerStatixSpec spec,
        ITermFactory termFactory,
        LoggerFactory loggerFactory
    ) {
        this.spec = spec;
        this.termFactory = termFactory;
        this.strategoTerms = new StrategoTerms(termFactory);
        this.log = loggerFactory.create(TigerAnalyzer.class);
    }

    public SolverContext createContext() {
        return new SolverContext(spec.spec);
    }

    public SolverState analyze(SolverContext ctx, IStrategoTerm ast, @Nullable ResourceKey resourceKey) throws InterruptedException {
        PlaceholderVarMap placeholderVarMap = new PlaceholderVarMap(resourceKey != null ? resourceKey.toString() : "<unknown resource>");
        IConstraint rootConstraint = getRootConstraint(ast, resourceKey, "static-semantics", placeholderVarMap);   /* TODO: Get the spec name from the spec? */
        return analyze(ctx, spec.spec, rootConstraint);
    }

    /**
     * Gets the root constraint of the specification.
     *
     * @return the root constraint
     */
    private IConstraint getRootConstraint(IStrategoTerm ast, @Nullable ResourceKey resourceKey, String specName, PlaceholderVarMap placeholderVarMap) {
        String rootRuleName = "programOK";      // FIXME: Ability to specify root rule somewhere
        String qualifiedName = makeQualifiedName(specName, rootRuleName);
        // TODO? <stx--explode> statixAst
        return new CUser(qualifiedName, Collections.singletonList(toStatixAst(ast, resourceKey, placeholderVarMap)), null);
    }

    /**
     * Converts a Stratego AST to a Statix AST.
     *
     * @param ast the Stratego AST to convert
     * @param resourceKey the resource key of the resource from which the AST was parsed
     * @return the resulting Statix AST, annotated with term indices
     */
    private ITerm toStatixAst(IStrategoTerm ast, @Nullable ResourceKey resourceKey, PlaceholderVarMap placeholderVarMap) {
        IStrategoTerm annotatedAst = addIndicesToAst(ast, resourceKey);
        ITerm statixAst = strategoTerms.fromStratego(annotatedAst);
        ITerm newStatixAst = replacePlaceholdersByConstraintVariables(statixAst, placeholderVarMap);
        return newStatixAst;
    }

    private ITerm replacePlaceholdersByConstraintVariables(ITerm term, PlaceholderVarMap placeholderVarMap) {
        return term.match(Terms.<ITerm>casesFix(
            (m, appl) ->  {
                if (appl.getOp().endsWith("-Plhdr") && appl.getArity() == 0) {
                    // Placeholder
                    return placeholderVarMap.addPlaceholderMapping(appl);
                } else {
                    return TermBuild.B.newAppl(appl.getOp(), appl.getArgs().stream().map(a -> a.match(m)).collect(Collectors.toList()), appl.getAttachments());
                }
            },
            (m, list) -> list.match(ListTerms.<IListTerm>casesFix(
                (lm, cons) -> TermBuild.B.newCons(cons.getHead().match(m), cons.getTail().match(lm), cons.getAttachments()),
                (lm, nil) -> nil,
                (lm, var) -> var
            )),
            (m, string) -> string,
            (m, integer) -> integer,
            (m, blob) -> blob,
            (m, var) -> var
        ));
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
     * Annotates the terms of the AST with term indices.
     *
     * @param ast the AST
     * @param resourceKey the resource key from which the AST was created
     * @return the annotated AST
     */
    private IStrategoTerm addIndicesToAst(IStrategoTerm ast, ResourceKey resourceKey) {
        return StrategoTermIndices.index(ast, resourceKey.toString(), termFactory);
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
