package mb.statix.common;

import com.google.common.collect.ImmutableList;
import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.util.CapsuleUtil;
import mb.resource.ResourceKey;
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
import java.util.Collections;
import java.util.function.Function;

/**
 * Statix-based semantic analyzer.
 */
public class StatixAnalyzer {

//    private final Logger log;
    private final StrategoTerms strategoTerms;
    private final ITermFactory termFactory;
    private final Spec spec;

    @Inject public StatixAnalyzer(
//        LoggerFactory loggerFactory,
        ITermFactory termFactory,
        Spec spec
    ) {
//        this.log = loggerFactory.create(getClass());
        this.strategoTerms = new StrategoTerms(termFactory);
        this.termFactory = termFactory;
        this.spec = spec;
    }

    /**
     * Performs analysis on the given root resource.
     *
     * @param rootResource the resource key of the root resource to analyze
     * @param astProvider given a resource key, provides the AST of the resource
     * @return the analysis results
     */
    public ASolverResult analyze(ResourceKey rootResource, Function<ResourceKey, IStrategoTerm> astProvider) throws InterruptedException {
        // TODO: Multi-file support. For now, we just get the root resource as the only resource.
        @SuppressWarnings("UnnecessaryLocalVariable") ResourceKey resourceKey = rootResource;
        IStrategoTerm ast = astProvider.apply(resourceKey);
        ITerm statixAst = toStatixAst(ast, resourceKey);

        IConstraint constraint = getRootConstraint(statixAst);

        return Solver.solve(
            this.spec,
            State.of(this.spec),
            CapsuleUtil.toSet(ImmutableList.of(constraint)),
            io.usethesource.capsule.Map.Immutable.of(),
            Completeness.Transient.of(this.spec).freeze(),
            new NullDebugContext()
        );
    }

    /**
     * Gets the root constraint to be solved for the program.
     *
     * @return the root constraint
     */
    private CUser getRootConstraint(ITerm ast) {
        String rootRuleName = "programOK";      // FIXME: Ability to specify root rule somewhere
        String qualifiedName = makeQualifiedName("", rootRuleName);
        // TODO? <stx--explode> statixAst
        return new CUser(qualifiedName, Collections.singletonList(ast), null);
    }
//
//    /**
//     * Gets the Statix specification of the language.
//     *
//     * @return the Statix specification
//     */
//    private Spec getStatixSpec() {
//        /*
//  stx--language-spec-by-name =
//    MkSingleton                 // ![<id>]
//  ; language-resources(stx--module-path, stx--spec-imports)
//  ; map(Snd)
//  ; stx--merge-spec-aterms
//         */
//        Spec spec = null;
//        if (!checkNoOverlappingRules(spec)) {
//            // Analysis failed
//            return null;
//        }
//        return spec;
//    }

    /**
     * Converts a Stratego AST to a Statix AST.
     *
     * @param ast the Stratego AST to convert
     * @param resourceKey the resource key of the resource from which the AST was parsed
     * @return the resulting Statix AST, annotated with term indices
     */
    private ITerm toStatixAst(IStrategoTerm ast, ResourceKey resourceKey) {
        IStrategoTerm annotatedAst = addIndicesToAst(ast, resourceKey);
        return strategoTerms.fromStratego(annotatedAst);
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



}
