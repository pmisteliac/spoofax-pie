package mb.spoofax.intellij.editor;

import com.google.common.collect.Lists;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import mb.common.region.Region;
import mb.common.token.IdentifierTokenKind;
import mb.common.token.KeywordTokenKind;
import mb.common.token.LayoutTokenKind;
import mb.common.token.NumberTokenKind;
import mb.common.token.OperatorTokenKind;
import mb.common.token.StringTokenKind;
import mb.common.token.TokenImpl;
import mb.common.token.TokenType;
import mb.common.token.UnknownTokenKind;
import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.pie.api.ExecException;
import mb.pie.api.PieSession;
import mb.pie.api.Task;
import mb.resource.ReadableResource;
import mb.resource.ResourceKey;
import mb.resource.ResourceService;
import mb.spoofax.core.language.LanguageInstance;
import mb.spoofax.intellij.ScopeNames;
import mb.spoofax.intellij.psi.SpoofaxTokenTypeManager;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lexer for Spoofax languages in IntelliJ.
 */
public final class SpoofaxLexer extends LexerBase {
    private final ResourceKey resourceKey;

    private final Logger logger;
    private final SpoofaxTokenTypeManager tokenTypeManager;
    private final ScopeManager scopeManager;
    private final ResourceService resourceService;
    private final Provider<PieSession> pieSessionProvider;
    private final LanguageInstance languageInstance;

    @Nullable private CharSequence buffer = null;
    private int startOffset = 0;
    private int endOffset = 0;
    private List<SpoofaxIntellijToken> tokens = Collections.emptyList();
    private int tokenIndex = 0;


    /**
     * Factory class.
     */
    public static class Factory {
        private final LoggerFactory loggerFactory;
        private final SpoofaxTokenTypeManager tokenTypeManager;
        private final ScopeManager scopeManager;
        private final ResourceService resourceService;
        private final Provider<PieSession> pieSessionProvider;
        private final LanguageInstance languageInstance;

        @Inject public Factory(
            LoggerFactory loggerFactory,
            SpoofaxTokenTypeManager tokenTypeManager,
            ScopeManager scopeManager,
            ResourceService resourceService,
            Provider<PieSession> pieSessionProvider,
            LanguageInstance languageInstance
        ) {
            this.loggerFactory = loggerFactory;
            this.tokenTypeManager = tokenTypeManager;
            this.scopeManager = scopeManager;
            this.resourceService = resourceService;
            this.pieSessionProvider = pieSessionProvider;
            this.languageInstance = languageInstance;
        }

        public SpoofaxLexer create(ResourceKey resourceKey) {
            return new SpoofaxLexer(
                resourceKey,
                loggerFactory,
                tokenTypeManager,
                scopeManager,
                resourceService,
                pieSessionProvider,
                languageInstance);
        }
    }


    /**
     * Initializes a new instance of the {@link SpoofaxLexer} class.
     */
    private SpoofaxLexer(
        ResourceKey resourceKey,
        LoggerFactory loggerFactory,
        SpoofaxTokenTypeManager tokenTypeManager,
        ScopeManager scopeManager,
        ResourceService resourceService,
        Provider<PieSession> pieSessionProvider,
        LanguageInstance languageInstance
    ) {
        this.resourceKey = resourceKey;
        this.logger = loggerFactory.create(getClass());
        this.tokenTypeManager = tokenTypeManager;
        this.scopeManager = scopeManager;
        this.resourceService = resourceService;
        this.pieSessionProvider = pieSessionProvider;
        this.languageInstance = languageInstance;
    }


    @Override public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        assert initialState == 0;
        assert 0 <= startOffset && startOffset <= buffer.length() : "Start offset " + endOffset + " out of range 0-" + buffer.length();
        assert 0 <= endOffset && endOffset <= buffer.length() : "End offset " + endOffset + " out of range 0-" + buffer.length();

        logger.debug("Lexing " + this.resourceKey);

        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenIndex = 0;

        if(buffer.length() == 0) {
            logger.debug("Buffer is empty.");
            this.tokens = Collections.emptyList();
        } else {
            // GK: what is syntax coloring information doing here?
            try(final PieSession session = this.pieSessionProvider.get()) {
                final Task<@Nullable ArrayList<mb.common.token.Token>> tokenizerTask =
                    this.languageInstance.createTokenizerTask(this.resourceKey);
                @Nullable List<mb.common.token.Token> resourceTokens = session.requireTopDown(tokenizerTask);
                if(resourceTokens == null)
                    resourceTokens = getDefaultTokens(this.resourceKey);
                logger.debug("Tokenizer task returned {} tokens", resourceTokens.size());
                this.tokens = tokenize(resourceTokens);
            } catch(ExecException e) {
                throw new RuntimeException("Styling resource '" + this.resourceKey + "' failed unexpectedly", e);
            }
        }
        logger.debug("Tokenizer produced {} tokens", this.tokens.size());
    }

    /**
     * Gets the default tokens that cover the resource.
     *
     * @param resourceKey The resource key.
     * @return The default tokens for the resource.
     */
    private List<mb.common.token.Token> getDefaultTokens(ResourceKey resourceKey) {
        final ReadableResource resource = this.resourceService.getReadableResource(resourceKey);
        try {
            int length = (int) resource.getSize();
            return Lists.newArrayList(
                new TokenImpl(new Region(0, length), new UnknownTokenKind(), null)
            );
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tokenizes a list of Spoofax tokens into IntelliJ tokens.
     *
     * @param tokens The list of tokens to tokenize.
     * @return The list of IntelliJ tokens that represents the input tokens for IntelliJ.
     */
    private List<SpoofaxIntellijToken> tokenize(List<mb.common.token.Token> tokens) {
        List<SpoofaxIntellijToken> newTokens = new ArrayList<>();
        int offset = 0;

        for(mb.common.token.Token token : tokens) {
            int tokenStart = token.getRegion().startOffset;
            int tokenEnd = token.getRegion().endOffset + 1;        // End offsets are exclusive.

            // We assume that tokens are non-empty. When we encounter
            // a token with an end at or before its start,
            // it gets ignored.
            if(tokenEnd <= tokenStart) continue;

            // We assume the list of tokens is ordered by value.
            // When we encounter a token that's before the current
            // `value`, it gets ignored.
            // We assume that no tokens overlap. When we encounter a
            // token that starts before the previous token ends,
            // it gets ignored.
            if(tokenStart < offset) continue;

            // We assume that tokens cover all characters. When we
            // encounter a stretch of characters not covered by a
            // token, we assign it our own dummy token/element.
            if(offset < tokenStart) {
                // Add dummy element.
                offset = addTokenElement(newTokens, null, offset, tokenStart);
            }

            assert offset == tokenStart;

            // Add element.
            offset = addTokenElement(newTokens, token, offset, tokenEnd);

            // When we've seen tokens up to the end of the highlighted range
            // we bail out.
            if(offset >= this.endOffset)
                break;
        }

        // When there is a gap between the last token and the end of the highlighted range
        // we insert our own dummy token/element.
        if(offset < this.endOffset) {
            offset = addTokenElement(newTokens, null, offset, this.endOffset);
        }

        assert offset >= this.endOffset;

        return newTokens;
    }

    /**
     * Adds a token to the list of IntelliJ tokens.
     *
     * @param tokenList   The list of IntelliJ tokens to modify.
     * @param token       The token to transform and add.
     * @param startOffset The inclusive zero-based start offset of the token.
     * @param endOffset   The exclusive zero-based end offset of the token.
     * @return The new end offset.
     */
    // Apparently this means @Nullable mb.common.token.Token in Java :/
    private int addTokenElement(List<SpoofaxIntellijToken> tokenList, mb.common.token.@Nullable Token token, int startOffset, int endOffset) {
        IElementType tokenType = getTokenType(token);
        tokenList.add(new SpoofaxIntellijToken(startOffset, endOffset, tokenType));
        return endOffset;
    }

    /**
     * Gets the token type for the specified Spoofax token.
     *
     * @param token The Spoofax token.
     * @return The corresponding token type.
     */
    private IElementType getTokenType(mb.common.token.@Nullable Token token) {
        final String simplfiedScopeName;
        if(token != null) {
            final ScopeNames scopeNames = getScopeNamesFromType(token.getType());
            simplfiedScopeName = this.scopeManager.getSimplifiedScope(scopeNames);
        } else {
            simplfiedScopeName = this.scopeManager.DEFAULT_SCOPE;
        }
        return this.tokenTypeManager.getTokenType(simplfiedScopeName);
    }

    /**
     * Determines the scope name for the given token type.
     * <p>
     * This is a compatibility method for changing between Spoofax TokenTypes and ScopeNames. In the future, once all
     * Spoofax tokens support scope names, this method should be removed.
     *
     * @param tokenType The token type.
     * @return The associated scope name.
     */
    private ScopeNames getScopeNamesFromType(@Nullable TokenType tokenType) {
        // @formatter:off
        if (tokenType instanceof IdentifierTokenKind) return new ScopeNames("entity");
        if (tokenType instanceof KeywordTokenKind)    return new ScopeNames("keyword");
        if (tokenType instanceof LayoutTokenKind)     return new ScopeNames("comment");
        if (tokenType instanceof NumberTokenKind)     return new ScopeNames("constant.numeric");
        if (tokenType instanceof OperatorTokenKind)   return new ScopeNames("keyword.operator");
        if (tokenType instanceof StringTokenKind)     return new ScopeNames("string");
        // @formatter:on
        return new ScopeNames(scopeManager.DEFAULT_SCOPE);
    }

    @Override
    public int getState() {
        return 0;
    }


    @Override
    public @Nullable IElementType getTokenType() {
        if(0 <= this.tokenIndex && this.tokenIndex < this.tokens.size())
            return this.tokens.get(tokenIndex).tokenType;
        else
            return null;
    }

    @Override
    public int getTokenStart() {
        assert 0 <= this.tokenIndex && this.tokenIndex < this.tokens.size() : "Expected index 0 <= $tokenIndex < ${tokens.size}.";
        return this.tokens.get(this.tokenIndex).startOffset;
    }

    @Override
    public int getTokenEnd() {
        assert 0 <= this.tokenIndex && this.tokenIndex < this.tokens.size() : "Expected index 0 <= $tokenIndex < ${tokens.size}.";
        return this.tokens.get(this.tokenIndex).endOffset;
    }

    @Override
    public void advance() {
        this.tokenIndex++;
    }

    public int getBufferStart() {
        return this.startOffset;
    }

    @Override
    public CharSequence getBufferSequence() {
        assert this.buffer != null;
        return this.buffer;
    }

    @Override
    public int getBufferEnd() {
        return this.endOffset;
    }

    private static class SpoofaxIntellijToken {
        private final int startOffset;
        private final int endOffset;
        private IElementType tokenType;

        public SpoofaxIntellijToken(int startOffset, int endOffset, IElementType tokenType) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.tokenType = tokenType;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public IElementType getTokenType() {
            return tokenType;
        }
    }
}