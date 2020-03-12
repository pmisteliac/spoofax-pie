package mb.statix.codecompletion;

import io.usethesource.capsule.Map;
import mb.jsglr.common.MoreTermUtils;
import mb.log.slf4j.SLF4JLoggerFactory;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.PlaceholderVarMap;
import mb.nabl2.terms.stratego.StrategoPlaceholders;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.resource.DefaultResourceKey;
import mb.resource.ResourceKey;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.common.StatixAnalyzer;
import mb.statix.common.StatixSpec;
import org.junit.jupiter.api.Test;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that the completion algorithm is complete.
 * For a given AST, it must be able to regenerate that AST in a number of completion steps,
 * when presented with the AST with a hole in it.
 */
public class CompletenessTest {

    @Test
    public void doTest() throws InterruptedException, IOException {
        ITermFactory termFactory = new TermFactory();
        StrategoTerms strategoTerms = new StrategoTerms(termFactory);
        StatixSpec spec = StatixSpec.fromClassLoaderResources(CompletenessTest.class, "mb/statix/codecompletion/spec.aterm");
        ResourceKey resourceKey = new DefaultResourceKey("test", "ast");
        IStrategoTerm ast = MoreTermUtils.fromClassLoaderResources(CompletenessTest.class, "mb/statix/codecompletion/test1.aterm");
        SLF4JLoggerFactory loggerFactory = new SLF4JLoggerFactory();
        TermCompleter completer = new TermCompleter();
        StatixAnalyzer analyzer = new StatixAnalyzer(spec, termFactory, loggerFactory);
        AtomicInteger varI = new AtomicInteger();
        AstShooter shooter = new AstShooter(new Random(1337), resourceKey.toString(), () -> "v" + varI.getAndIncrement());

        // Get a Statix AST with term variables
        IStrategoTerm annotatedAst = StrategoTermIndices.index(ast, resourceKey.toString(), termFactory);
        ITerm tmpStatixAst = strategoTerms.fromStratego(annotatedAst);
        PlaceholderVarMap placeholderVarMap = new PlaceholderVarMap(resourceKey.toString());
        ITerm statixAst = StrategoPlaceholders.replacePlaceholdersByVariables(tmpStatixAst, placeholderVarMap);

        // Preparation
        ImmutableCompletionExpectation<? extends ITerm> completionExpectation = shooter.shootHoleInTerm(statixAst, 3);
        // Get the solver state of the program (whole project),
        // which should have some remaining constraints on the placeholders.
        SolverContext ctx = analyzer.createContext();
//        SolverState initialState = analyzer.analyze(ctx, completionExpectation.getIncompleteAst(), Collections.emptyList());
        SolverState initialState = analyzer.analyze(ctx, completionExpectation.getIncompleteAst(), completionExpectation.getVars());
        if (initialState.hasErrors()) {
            fail("Completion failed: input program validation failed.\n" + initialState.toString());
            return;
        }
        if (initialState.getConstraints().isEmpty()) {
            fail("Completion failed: no constraints left, nothing to complete.\n" + initialState.toString());
            return;
        }

        completionExpectation = completionExpectation.withState(initialState);
        while (!completionExpectation.isComplete()) {
            // For each term variable, invoke completion
            for(ITermVar var : completionExpectation.getVars()) {
                // TODO: Use a new state with the correct existentials, namely those in completionExpectation.getVars()
                SolverState state = Objects.requireNonNull(completionExpectation.getState())
//                SolverState state = Objects.requireNonNull(completionExpectation.getState()).withExistentials(completionExpectation.getVars());
                List<TermCompleter.CompletionSolverProposal> proposals = completer.complete(ctx, state, var);
                // For each proposal, find the candidates that fit
                final ImmutableCompletionExpectation<? extends ITerm> currentCompletionExpectation = completionExpectation;
                final List<ImmutableCompletionExpectation<? extends ITerm>> candidates = proposals.stream().map(p -> currentCompletionExpectation.tryReplace(var, p)).filter(Objects::nonNull).collect(Collectors.toList());
                if(candidates.size() == 1) {
                    // Only one candidate, let's apply it
                    completionExpectation = candidates.get(0);
                } else if(candidates.size() > 1) {
                    // Multiple candidates, let's use the first one
                    // (could also use the biggest one instead)
                    completionExpectation = candidates.get(0);
                } else {
                    // No candidates, completion algorithm is not complete
                    fail(() -> "Could not complete var " + var + " in AST:\n  " + currentCompletionExpectation.getIncompleteAst() + "\n" +
                        "Expected:\n  " + currentCompletionExpectation.getExpectations().get(var) + "\n" +
                        "Got proposals:\n  " + proposals.stream().map(p -> p.getTerm().toString()).collect(Collectors.joining("\n  ")));
                    return;
                }
            }
        }

        // Done! Success!
    }

}
