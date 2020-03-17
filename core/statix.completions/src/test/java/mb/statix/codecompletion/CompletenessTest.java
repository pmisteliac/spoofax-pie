package mb.statix.codecompletion;

import mb.jsglr.common.MoreTermUtils;
import mb.log.api.Logger;
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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that the completion algorithm is complete.
 * For a given AST, it must be able to regenerate that AST in a number of completion steps,
 * when presented with the AST with a hole in it.
 */
@SuppressWarnings("SameParameterValue")
public class CompletenessTest {

    private static final SLF4JLoggerFactory loggerFactory = new SLF4JLoggerFactory();
    private static final Logger log = loggerFactory.create(CompletenessTest.class);
    private static final String TESTPATH = "mb/statix/codecompletion";
    private static final String TIGER_SPEC_PATH = TESTPATH + "/spec.aterm";

    @TestFactory
    public List<DynamicTest> completenessTests() {
        //noinspection ArraysAsListWithZeroOrOneArgument
        return Arrays.asList(
            completenessTest(TESTPATH + "/test1.aterm", TESTPATH + "/test1.input.aterm", TIGER_SPEC_PATH),
            completenessTest(TESTPATH + "/test2.aterm", TESTPATH + "/test2.input.aterm", TIGER_SPEC_PATH)
        );
    }

    private DynamicTest completenessTest(String expectedTermPath, String inputTermPath, String specPath) {
        return DynamicTest.dynamicTest("complete file " + Paths.get(inputTermPath).getFileName() + " to " + Paths.get(expectedTermPath).getFileName() + " using spec " + Paths.get(specPath).getFileName() + "",
            () -> {
                StatixSpec spec = StatixSpec.fromClassLoaderResources(CompletenessTest.class, specPath);
                IStrategoTerm expectedTerm = MoreTermUtils.fromClassLoaderResources(CompletenessTest.class, expectedTermPath);
                IStrategoTerm inputTerm = MoreTermUtils.fromClassLoaderResources(CompletenessTest.class, inputTermPath);
                doCompletenessTest(expectedTerm, inputTerm, spec);
            });
    }

    private void doCompletenessTest(IStrategoTerm expectedTerm, IStrategoTerm inputTerm, StatixSpec spec) throws InterruptedException, IOException {
        ITermFactory termFactory = new TermFactory();
        StrategoTerms strategoTerms = new StrategoTerms(termFactory);
        ResourceKey resourceKey = new DefaultResourceKey("test", "ast");

        IStrategoTerm annotatedExpectedTerm = StrategoTermIndices.index(expectedTerm, resourceKey.toString(), termFactory);
        ITerm expectedStatixTerm = strategoTerms.fromStratego(annotatedExpectedTerm);
//        PlaceholderVarMap placeholderVarMap = new PlaceholderVarMap(resourceKey.toString());
//        ITerm statixAst = StrategoPlaceholders.replacePlaceholdersByVariables(tmpStatixAst, placeholderVarMap);

        IStrategoTerm annotatedInputTerm = StrategoTermIndices.index(inputTerm, resourceKey.toString(), termFactory);
        ITerm inputStatixTerm = strategoTerms.fromStratego(annotatedInputTerm);

        doCompletenessTest(expectedStatixTerm, inputStatixTerm, spec, termFactory, resourceKey);
    }

//    private void doCompletenessTestRandom(ITerm expectedTerm, StatixSpec spec, ITermFactory termFactory, ResourceKey resourceKey) throws InterruptedException, IOException {
//        AtomicInteger varI = new AtomicInteger();
//        AstShooter shooter = new AstShooter(new Random(1337), resourceKey.toString(), () -> "v" + varI.getAndIncrement());
//        ImmutableCompletionExpectation<? extends ITerm> completionExpectation = shooter.shootHoleInTerm(expectedTerm, 3);
//        doCompletenessTest(expectedTerm, completionExpectation.getIncompleteAst(), spec, termFactory, resourceKey);
//    }

    private void doCompletenessTest(ITerm expectedTerm, ITerm inputTerm, StatixSpec spec, ITermFactory termFactory, ResourceKey resourceKey) throws InterruptedException, IOException {
//        ITermFactory termFactory = new TermFactory();
//        StrategoTerms strategoTerms = new StrategoTerms(termFactory);
//        StatixSpec spec = StatixSpec.fromClassLoaderResources(CompletenessTest.class, specPath);//"mb/statix/codecompletion/spec.aterm");
//        ResourceKey resourceKey = new DefaultResourceKey("test", "ast");
//        IStrategoTerm expectedAst = MoreTermUtils.fromClassLoaderResources(CompletenessTest.class, inputAtermPath);//"mb/statix/codecompletion/test1.aterm");
        TermCompleter completer = new TermCompleter();
        StatixAnalyzer analyzer = new StatixAnalyzer(spec, termFactory, loggerFactory);
//        AtomicInteger varI = new AtomicInteger();
//        AstShooter shooter = new AstShooter(new Random(1337), resourceKey.toString(), () -> "v" + varI.getAndIncrement());

        // Get a Statix AST with term variables
//        IStrategoTerm annotatedAst = StrategoTermIndices.index(expectedTerm, resourceKey.toString(), termFactory);
//        ITerm tmpStatixAst = strategoTerms.fromStratego(annotatedAst);
//        PlaceholderVarMap placeholderVarMap = new PlaceholderVarMap(resourceKey.toString());
//        ITerm statixAst = StrategoPlaceholders.replacePlaceholdersByVariables(tmpStatixAst, placeholderVarMap);

        // Preparation
        long prepStartTime = System.nanoTime();
        ImmutableCompletionExpectation<? extends ITerm> completionExpectation = ImmutableCompletionExpectation.fromTerm(inputTerm, expectedTerm, resourceKey.toString());

//        ImmutableCompletionExpectation<? extends ITerm> completionExpectation = shooter.shootHoleInTerm(expectedTerm, 3);
        // Get the solver state of the program (whole project),
        // which should have some remaining constraints on the placeholders.
        SolverContext ctx = analyzer.createContext();
        long analyzeStartTime = System.nanoTime();
        SolverState initialState = analyzer.analyze(ctx, completionExpectation.getIncompleteAst());
        if (initialState.hasErrors()) {
            fail("Completion failed: input program validation failed.\n" + initialState.toString());
            return;
        }
        if (initialState.getConstraints().isEmpty()) {
            fail("Completion failed: no constraints left, nothing to complete.\n" + initialState.toString());
            return;
        }

        long completeStartTime = System.nanoTime();
        int stepCount = 0;
        completionExpectation = completionExpectation.withState(initialState);
        while (!completionExpectation.isComplete()) {
            // For each term variable, invoke completion
            for(ITermVar var : completionExpectation.getVars()) {
                SolverState state = Objects.requireNonNull(completionExpectation.getState());
                List<TermCompleter.CompletionSolverProposal> proposals = completer.complete(ctx, state, var);
                // For each proposal, find the candidates that fit
                final ImmutableCompletionExpectation<? extends ITerm> currentCompletionExpectation = completionExpectation;
                final List<ImmutableCompletionExpectation<? extends ITerm>> candidates = proposals.stream()
                    .map(p -> currentCompletionExpectation.tryReplace(var, p))
                    .filter(Objects::nonNull).collect(Collectors.toList());
                if(candidates.size() == 1) {
                    // Only one candidate, let's apply it
                    completionExpectation = candidates.get(0);
                } else if(candidates.size() > 1) {
                    // Multiple candidates, let's use the one with the least number of open variables
                    // and otherwise the first one (could also use the biggest one instead)
                    candidates.sort(Comparator.comparingInt(o -> o.getVars().size()));
                    completionExpectation = candidates.get(0);
                } else {
                    // No candidates, completion algorithm is not complete
                    fail(() -> "Could not complete var " + var + " in AST:\n  " + currentCompletionExpectation.getIncompleteAst() + "\n" +
                        "Expected:\n  " + currentCompletionExpectation.getExpectations().get(var) + "\n" +
                        "Got proposals:\n  " + proposals.stream().map(p -> p.getTerm().toString()).collect(Collectors.joining("\n  ")));
                    return;
                }
                stepCount += 1;
            }
        }

        // Done! Success!

        long totalPrepareTime = analyzeStartTime - prepStartTime;
        long totalAnalyzeTime = completeStartTime - analyzeStartTime;
        long totalCompleteTime = System.nanoTime() - completeStartTime;
        long avgDuration = totalCompleteTime / stepCount;
        log.info("Done! Completed {} steps in {} ms, avg. {} ms/step. (Preparation: {} ms, initial analysis: {} ms)", stepCount,
            String.format("%2d", TimeUnit.NANOSECONDS.toMillis(totalCompleteTime)),
            String.format("%2d", TimeUnit.NANOSECONDS.toMillis(avgDuration)),
            String.format("%2d", TimeUnit.NANOSECONDS.toMillis(totalPrepareTime)),
            String.format("%2d", TimeUnit.NANOSECONDS.toMillis(totalAnalyzeTime))
        );
    }

}
