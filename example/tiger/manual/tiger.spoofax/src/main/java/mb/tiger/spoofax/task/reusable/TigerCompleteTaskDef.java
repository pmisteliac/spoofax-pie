package mb.tiger.spoofax.task.reusable;

import mb.common.editing.TextEdit;
import mb.common.region.Region;
import mb.common.style.StyleName;
import mb.common.util.ListView;
import mb.completions.common.CompletionProposal;
import mb.completions.common.CompletionResult;
import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.StrategoPlaceholders;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.pie.api.ExecContext;
import mb.pie.api.ExecException;
import mb.pie.api.Function;
import mb.pie.api.Supplier;
import mb.pie.api.TaskDef;
import mb.resource.ResourceKey;
import mb.spoofax.core.language.LanguageScope;
import mb.statix.codecompletion.TermCompleter;
import mb.statix.common.SolverContext;
import mb.statix.common.SolverState;
import mb.nabl2.terms.stratego.PlaceholderVarMap;
import mb.tiger.TigerAnalyzer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.attachments.OriginAttachment;
import org.spoofax.terms.util.TermUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@LanguageScope
public class TigerCompleteTaskDef implements TaskDef<TigerCompleteTaskDef.Input, @Nullable CompletionResult> {

    public static class Input implements Serializable {
        public final ResourceKey resourceKey;
        public final int caretLocation;
        public final Supplier<@Nullable IStrategoTerm> astSupplier;
        public final Function<IStrategoTerm, @Nullable String> prettyPrinterFunction;

        public Input(
            ResourceKey resourceKey,
            int caretLocation,
            Supplier<IStrategoTerm> astSupplier,
            Function<IStrategoTerm, @Nullable String> prettyPrinterFunction) {
            this.resourceKey = resourceKey;
            this.caretLocation = caretLocation;
            this.astSupplier = astSupplier;
            this.prettyPrinterFunction = prettyPrinterFunction;
        }
    }

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
    ) {
        this.log = loggerFactory.create(TigerCompleteTaskDef.class);
        this.strategoTerms = strategoTerms;
        this.termFactory = termFactory;
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
        @Nullable IStrategoTerm ast = input.astSupplier.get(context);
        if (ast == null){
            log.error("Completion failed: we didn't get an AST.");
            return null;   // Cannot complete when we don't get an AST.
        }

        // 3) Find the placeholder closest to the caret <- that's the one we want to complete
        @Nullable IStrategoAppl placeholder = findPlaceholderAt(ast, input.caretLocation);
        if (placeholder == null) {
            log.error("Completion failed: we don't know the placeholder.");
            return null;   // Cannot complete when we don't know the placeholder.
        }

        // Convert to Statix AST
        IStrategoTerm annotatedAst = StrategoTermIndices.index(ast, input.resourceKey.toString(), termFactory);
        ITerm tmpStatixAst = strategoTerms.fromStratego(annotatedAst);
        PlaceholderVarMap placeholderVarMap = new PlaceholderVarMap(input.resourceKey.toString());
        ITerm statixAst = StrategoPlaceholders.replacePlaceholdersByVariables(tmpStatixAst, placeholderVarMap);
        ITermVar placeholderVar = Objects.requireNonNull(placeholderVarMap.getVar((IApplTerm)strategoTerms.fromStratego(placeholder)));

        // 4) Get the solver state of the program (whole project),
        //    which should have some remaining constraints on the placeholder.
        //    TODO: What to do when the file is semantically incorrect? Recovery?
        SolverContext ctx = analyzer.createContext();
        SolverState initialState = analyzer.analyze(ctx, statixAst);
        if (initialState.hasErrors()) {
            log.error("Completion failed: input program validation failed.\n" + initialState.toString());
            return null;    // Cannot complete when analysis fails.
        }
        if (initialState.getConstraints().isEmpty()) {
            log.error("Completion failed: no constraints left, nothing to complete.\n" + initialState.toString());
            return null;    // Cannot complete when there are no constraints left.
        }

        // 5) Invoke the completer on the solver state, indicating the placeholder for which we want completions
        // 6) Get the possible completions back, as a list of ASTs with new solver states
        List<IStrategoTerm> completionTerms = complete(ctx, initialState, placeholderVar, placeholderVarMap);

        // 7) Format each completion as a proposal, with pretty-printed text
        List<String> completionStrings = completionTerms.stream().map(t -> {
            try {
                // FIXME: We should remove the explicated injections, somehow
                // How do we know which are injections?
                @Nullable String prettyPrinted = prettyPrint(context, t, input.prettyPrinterFunction);
                return prettyPrinted != null ? prettyPrinted : t.toString();
            } catch(ExecException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        // 8) Insert the selected completion: insert the pretty-printed text in the code,
        //    and (maybe?) add the solver state to the current solver state
        List<CompletionProposal> completionProposals = completionStrings.stream().map(s -> createCompletionProposal(s, input.caretLocation)).collect(Collectors.toList());

        if (completionProposals.isEmpty()) {
            log.warn("Completion returned no completion proposals.");
        }

        return new CompletionResult(ListView.copyOf(completionProposals), true);
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
        String label = normalizeText(text);
        StyleName style = Objects.requireNonNull(StyleName.fromString("meta.template"));
        return new CompletionProposal(label, "", "", "", "", style, textEdits, false);
    }

    private String normalizeText(String text) {
        // Replace all sequences of layout with a single space
        return text.replaceAll("\\s+", " ");
    }

    /**
     * Returns the pretty-printed version of the specified term.
     *
     * @param context the execution context
     * @param term the term to pretty-print
     * @param prettyPrinterFunction the pretty-printer function
     * @return the pretty-printed term; or {@code null} when it failed
     */
    private @Nullable String prettyPrint(ExecContext context, IStrategoTerm term, Function<IStrategoTerm, String> prettyPrinterFunction) throws ExecException, InterruptedException {
        return prettyPrinterFunction.apply(context, term);
    }

    private List<IStrategoTerm> complete(SolverContext ctx, SolverState state, ITermVar placeholderVar, PlaceholderVarMap placeholderVarMap) throws InterruptedException {
        List<TermCompleter.CompletionSolverProposal> proposalTerms = completer.complete(ctx, state, placeholderVar);
        return proposalTerms.stream().map(p -> strategoTerms.toStratego(StrategoPlaceholders.replaceVariablesByPlaceholders(p.getTerm(), placeholderVarMap))).collect(Collectors.toList());
    }

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
        Optional<IStrategoAppl> maybePlaceholder = TermUtils.asAppl(term).filter(StrategoPlaceholders::isPlaceholder);
        if (maybePlaceholder.isPresent()) return maybePlaceholder.get();
        // Recurse into the term
        for (IStrategoTerm subterm : term.getAllSubterms()) {
            @Nullable IStrategoAppl nearbyPlaceholder = findPlaceholderAt(subterm, caretOffset);
            if (nearbyPlaceholder != null) return nearbyPlaceholder;
        }
        return null;
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
