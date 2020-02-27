package mb.tiger.spoofax.task.reusable;

import mb.common.editing.TextEdit;
import mb.common.region.Region;
import mb.common.style.StyleName;
import mb.common.util.ListView;
import mb.completions.common.CompletionProposal;
import mb.completions.common.CompletionResult;
import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.pie.api.ExecContext;
import mb.pie.api.Supplier;
import mb.pie.api.TaskDef;
import mb.resource.ResourceKey;
import mb.spoofax.core.language.LanguageScope;
import mb.statix.codecompletion.TermCompleter;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.statix.spec.Spec;
import mb.tiger.PlaceholderVarMap;
import mb.tiger.TigerAnalyzer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.attachments.OriginAttachment;
import org.spoofax.terms.util.TermUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@LanguageScope
public class TigerCompleteTaskDef implements TaskDef<TigerCompleteTaskDef.Input, @Nullable CompletionResult> {

    public static class Input implements Serializable {
        public final ResourceKey resourceKey;
//        public final Supplier<@Nullable Spec> specProvider;
        public final Supplier<@Nullable IStrategoTerm> astProvider;
        public final int caretLocation;

        public Input(ResourceKey resourceKey, /*Supplier<@Nullable Spec> specProvider,*/ Supplier<IStrategoTerm> astProvider, int caretLocation) {
            this.resourceKey = resourceKey;
//            this.specProvider = specProvider;
            this.astProvider = astProvider;
            this.caretLocation = caretLocation;
        }
    }

//    public static class Output implements Serializable {
//
//    }

//    private final TigerAnalyze analyzeTask;
//    private final StatixAnalyzer statixAnalyzer;
    private final Logger log;
    private final TermCompleter completer = new TermCompleter();
    private final StrategoTerms strategoTerms;
    private final ITermFactory termFactory;
    private final Provider<TigerAnalyzer> analyzerProvider;

    @Inject public TigerCompleteTaskDef(
        LoggerFactory loggerFactory,
        StrategoTerms strategoTerms,
        ITermFactory termFactory,
        Provider<TigerAnalyzer> analyzerProvider
//        TigerAnalyze analyzeTask
//        StatixAnalyzer statixAnalyzer
    ) {
        this.log = loggerFactory.create(TigerCompleteTaskDef.class);
        this.strategoTerms = strategoTerms;
        this.termFactory = termFactory;
//        this.statixAnalyzer = statixAnalyzer;
//        this.analyzeTask = analyzeTask;
        this.analyzerProvider = analyzerProvider;
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @Override
    public @Nullable CompletionResult exec(ExecContext context, Input input) throws Exception {
        TigerAnalyzer analyzer = analyzerProvider.get();

        // 1) Get the file in which code completion is invoked & parse the file with syntactic completions enabled,
        //    resulting in an AST with placeholders
        //    ==> This should be done by specifying the correct astProvider
        // TODO: get the ast in 'completion mode', with placeholders (use placeholder recovery or inference)
        @Nullable IStrategoTerm ast = input.astProvider.get(context);
        if (ast == null){
            log.error("Completion failed: we didn't get an AST.");
            return null;   // Cannot complete when we don't get an AST.
        }

        // 3) Get the solver state of the program (whole project),
        //    which should have some remaining constraints on the placeholder.
        //    TODO: What to do when the file is semantically incorrect? Recovery?
        SolverContext ctx = analyzer.createContext();
        SolverState initialState = analyzer.analyze(ctx, ast, input.resourceKey);
        if (initialState.hasErrors()) {
            log.error("Completion failed: input program validation failed.\n" + initialState.toString());
            return null;    // Cannot complete when analysis fails.
        }
        if (initialState.getConstraints().isEmpty()) {
            log.error("Completion failed: no constraints left, nothing to complete.\n" + initialState.toString());
            return null;    // Cannot complete when there are no constraints left.
        }

        // TODO: Move getting the placeholder before analyze, for optimization (it is currently here for debugging)
        // 4) Find the placeholder closest to the caret <- that's the one we want to complete
        @Nullable IStrategoAppl placeholder = findPlaceholderAt(ast, input.caretLocation);
        if (placeholder == null) {
            log.error("Completion failed: we don't know the placeholder.");
            return null;   // Cannot complete when we don't know the placeholder.
        }

        // 5) Invoke the completer on the solver state, indicating the placeholder for which we want completions
        // 6) Get the possible completions back, as a list of ASTs with new solver states
        List<IStrategoTerm> completionTerms = complete(ctx, initialState, placeholder);

        // 7) Format each completion as a proposal, with pretty-printed text
        List<String> completionStrings = completionTerms.stream().map(this::prettyPrint).collect(Collectors.toList());

        // 8) Insert the selected completion: insert the pretty-printed text in the code,
        //    and (maybe?) add the solver state to the current solver state
        List<CompletionProposal> completionProposals = completionStrings.stream().map(s -> createCompletionProposal(s, input.caretLocation)).collect(Collectors.toList());

        if (completionProposals.isEmpty()) {
            log.warn("Completion returned no completion proposals.");
        }

        return new CompletionResult(ListView.copyOf(completionProposals), true);
//        return new CompletionResult(ListView.of(
//            new CompletionProposal("mypackage", "description", "", "", "mypackage", Objects.requireNonNull(StyleName.fromString("meta.package")), ListView.of(), false),
//            new CompletionProposal("myclass", "description", "", "T", "mypackage", Objects.requireNonNull(StyleName.fromString("meta.class")), ListView.of(), false)
//        ), true);
    }

    /**
     * Creates a completion proposal.
     *
     * @param text the text to insert
     * @param caretOffset the caret location
     * @return the created proposal
     */
    private CompletionProposal createCompletionProposal(String text, int caretOffset) {
        ListView<TextEdit> textEdits = ListView.of(new TextEdit(Region.atOffset(caretOffset), text));
        StyleName style = Objects.requireNonNull(StyleName.fromString("meta.template"));
        return new CompletionProposal(text, "", "", "", "", style, textEdits, false);
    }

    /**
     * Returns the pretty-printed version of the specified term.
     *
     * @param term the term to pretty-print
     * @return the pretty-printed term
     */
    private String prettyPrint(IStrategoTerm term) {
        // TODO: Implement
        return term.toString();
    }

//    /**
//     * Gets the root constraint of the specification.
//     *
//     * @param spec the specification
//     * @return the root constraint
//     */
//    private IConstraint getRootConstraint(Spec spec, IStrategoTerm ast, ResourceKey resourceKey) {
//        String rootRuleName = "programOK";      // FIXME: Ability to specify root rule somewhere
//        String qualifiedName = makeQualifiedName("", rootRuleName);
//        // TODO? <stx--explode> statixAst
//        return new CUser(qualifiedName, Collections.singletonList(toStatixAst(ast, resourceKey)), null);
//    }

//    /**
//     * Converts a Stratego AST to a Statix AST.
//     *
//     * @param ast the Stratego AST to convert
//     * @param resourceKey the resource key of the resource from which the AST was parsed
//     * @return the resulting Statix AST, annotated with term indices
//     */
//    private ITerm toStatixAst(IStrategoTerm ast, ResourceKey resourceKey) {
//        IStrategoTerm annotatedAst = addIndicesToAst(ast, resourceKey);
//        return strategoTerms.fromStratego(annotatedAst);
//    }

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

//    /**
//     * Annotates the terms of the AST with term indices.
//     *
//     * @param ast the AST
//     * @param resourceKey the resource key from which the AST was created
//     * @return the annotated AST
//     */
//    private IStrategoTerm addIndicesToAst(IStrategoTerm ast, ResourceKey resourceKey) {
//        return StrategoTermIndices.index(ast, resourceKey.toString(), termFactory);
//    }

//    /**
//     * Invokes analysis.
//     *
//     * @param spec the Statix specification
//     * @param rootConstraint the root constraint
//     * @return the resulting analysis result; or {@code null} when it failed
//     */
//    private @Nullable SearchState analyze(SearchContext ctx, Spec spec, IConstraint rootConstraint) throws InterruptedException {
//        log.info("Preparing...");
//        SearchState startState = SearchState.of(spec, State.of(spec), ImmutableList.of(rootConstraint));
//        SearchState completionStartState = infer().apply(ctx, startState).findFirst().orElseThrow(() -> new IllegalStateException("This cannot be happening."));
//        if (completionStartState.hasErrors()) {
//            log.error("Input program validation failed. Aborted.\n" + completionStartState.toString());
//            return null;
//        }
//        if (completionStartState.getConstraints().isEmpty()) {
//            log.error("No constraints left, nothing to complete. Aborted.\n" + completionStartState.toString());
//            return null;
//        }
//        log.info("Ready.");
//        return completionStartState;
//    }
//
    private List<IStrategoTerm> complete(SolverContext ctx, SolverState state, IStrategoAppl placeholder) throws InterruptedException {
        return completer.complete(ctx, state, placeholder.getName()).stream().map(t -> strategoTerms.toStratego(replaceConstraintVariablesByPlaceholders(t))).collect(Collectors.toList());
    }

    private ITerm replaceConstraintVariablesByPlaceholders(ITerm term) {
        return term.match(Terms.<ITerm>casesFix(
            (m, appl) ->  TermBuild.B.newAppl(appl.getOp(), appl.getArgs().stream().map(a -> a.match(m)).collect(Collectors.toList()), appl.getAttachments()),
            (m, list) -> list.match(ListTerms.<IListTerm>casesFix(
                (lm, cons) -> TermBuild.B.newCons(cons.getHead().match(m), cons.getTail().match(lm), cons.getAttachments()),
                (lm, nil) -> nil,
                (lm, var) -> var
            )),
            (m, string) -> string,
            (m, integer) -> integer,
            (m, blob) -> blob,
            (m, var) -> {
                if (var.getName().startsWith("$")) {
                    return TermBuild.B.newAppl(var.getName().substring(1) + "-Phldr");
                } else if (var.getName().endsWith("-Plhdr")) {
                    return TermBuild.B.newAppl(var.getName().substring(1, var.getName().length() - (1 + "-Phldr".length())));
                } else {
                    return TermBuild.B.newAppl(var.getName() + "-Phldr");
                }
            }
        ));
    }

//    /**
//     * Invokes analysis on the given AST.
//     *
//     * @param context the task execution context
//     * @param ast the AST to analyze, with placeholders
//     * @param resourceKey the resource key
//     * @return the resulting analysis result; or {@code null} when it failed
//     */
//    private ASolverResult analyze(ExecContext context, IStrategoTerm ast, ResourceKey resourceKey) throws InterruptedException {
//        return statixAnalyzer.analyze(resourceKey, ignored -> ast);
//    }

//    /**
//     * Invokes analysis on the given AST.
//     *
//     * @param context the task execution context
//     * @param ast the AST to analyze, with placeholders
//     * @param resourceKey the resource key
//     * @return the resulting analysis result; or {@code null} when it failed
//     */
//    private ConstraintAnalyzer.@Nullable SingleFileResult analyze(ExecContext context, IStrategoTerm ast, ResourceKey resourceKey) throws InterruptedException, IOException, ExecException {
//        TigerAnalyze.@Nullable Output analysisResult = analyzeTask.exec(context, new TigerAnalyze.Input(resourceKey, ignored -> ast));
//        if (analysisResult == null) return null;
//        return analysisResult.result;
//    }

    /**
     * Finds the placeholder near the caret location in the specified term.
     *
     * This method assumes all terms in the term are uniquely identifiable,
     * for example through a term index or unique tree path.
     *
     * @param term the term (an AST with placeholders)
     * @param caretOffset the caret location
     * @return the placeholder; or {@code null} if not found
     */
    private @Nullable IStrategoAppl findPlaceholderAt(IStrategoTerm term, int caretOffset) {
        if (!termContainsCaret(term, caretOffset)) return null;
        Optional<IStrategoAppl> maybePlaceholder = TermUtils.asAppl(term).filter(TigerCompleteTaskDef::isPlaceholderTerm);
        if (maybePlaceholder.isPresent()) return maybePlaceholder.get();
        // Recurse into the term
        for (IStrategoTerm subterm : term.getAllSubterms()) {
            @Nullable IStrategoAppl nearbyPlaceholder = findPlaceholderAt(subterm, caretOffset);
            if (nearbyPlaceholder != null) return nearbyPlaceholder;
        }
        return null;
    }

    /**
     * Determines whether the given term is a placeholder term.
     *
     * @param term the term to check
     * @return {@code true} when the term is a placeholder term; otherwise, {@code false}
     */
    private static boolean isPlaceholderTerm(IStrategoAppl term) {
        IStrategoConstructor constructor = term.getConstructor();
        return constructor.getName().endsWith("-Plhdr") && constructor.getArity() == 0;
    }

    /**
     * Determines whether the specified term contains the specified caret offset.
     *
     * @param term the term
     * @param caretOffset the caret offset to find
     * @return {@code true} when the term contains the caret offset;
     * otherwise, {@code false}.
     */
    private boolean termContainsCaret(IStrategoTerm term, int caretOffset) {
        @Nullable ImploderAttachment imploder = getImploderAttachment(term);
        if(imploder == null) return false;

        // We get the zero-based offset of the first character in the token
        int startOffset = imploder.getLeftToken().getStartOffset();
        // We get the zero-based offset of the character following the token, which is why we have to add 1
        int endOffset = imploder.getRightToken().getEndOffset() + 1;
        // If the token is empty or malformed, we skip it. (An empty token cannot contain a caret anyway.)
        if (endOffset <= startOffset) return false;

        Region termRegion = Region.fromOffsets(
            startOffset,
            endOffset
        );
        return termRegion.contains(caretOffset);
    }

    /**
     * Gets the imploder attachment of the specified term.
     *
     * @param term the term for which to get the imploder attachment
     * @return the imploder attachment; or {@code null} if not found
     */
    private @Nullable ImploderAttachment getImploderAttachment(IStrategoTerm term) {
        @Nullable ImploderAttachment imploder = ImploderAttachment.get(term);
        if(imploder == null) {
            @Nullable IStrategoTerm originTerm = OriginAttachment.getOrigin(term);
            imploder = originTerm != null ? ImploderAttachment.get(originTerm) : null;
        }
        return imploder;
    }
}
