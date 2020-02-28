package mb.spoofax.eclipse.editor;

import mb.common.editing.TextEdit;
import mb.common.region.Region;
import mb.completions.common.CompletionProposal;
import mb.completions.common.CompletionResult;
import mb.pie.api.ExecException;
import mb.pie.api.PieSession;
import mb.pie.api.Task;
import mb.resource.ResourceKey;
import mb.spoofax.core.language.LanguageInstance;
import mb.spoofax.eclipse.EclipseLanguageComponent;
import mb.spoofax.eclipse.resource.EclipseResourcePath;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import javax.inject.Provider;

/**
 * Spoofax content assistant, used to provide code completion and parameter information.
 */
public abstract class SpoofaxContentAssistProcessor implements IContentAssistProcessor {

    public interface Factory {
        SpoofaxContentAssistProcessor create(SpoofaxEditor editor);
    }

    private final SpoofaxEditor editor;
    private final LanguageInstance languageInstance;
    private final Provider<PieSession> pieSessionProvider;

    private @Nullable String lastErrorMessage = null;

    protected SpoofaxContentAssistProcessor(
        SpoofaxEditor editor,
        LanguageInstance languageInstance,
        Provider<PieSession> pieSessionProvider
//        EclipseLanguageComponent languageComponent
    ) {
        this.editor = editor;
        this.languageInstance = languageInstance;
//        this.languageInstance = languageComponent.getLanguageInstance();
        this.pieSessionProvider = pieSessionProvider;
//        this.pieSessionProvider = languageComponent::newPieSession;
    }

    @Override
    public @Nullable ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        this.lastErrorMessage = null;

        final ResourceKey resourceKey = getResourceKeyFromEditor();
        final Region selection = Region.atOffset(offset);
        final @Nullable CompletionResult completionResult;
        try (final PieSession session = this.pieSessionProvider.get()) {
            Task<@Nullable CompletionResult> completionTask = this.languageInstance.createCompleteTask(resourceKey, selection);
            completionResult = session.require(completionTask);
        } catch (ExecException e) {
            this.lastErrorMessage = "Code completion on resource '" + resourceKey + "' failed unexpectedly.";
            throw new RuntimeException("Code completion on resource '" + resourceKey + "' failed unexpectedly.", e);
        }
        if (completionResult == null) {
            this.lastErrorMessage = "The code completion task failed.";
            return null;
        }

        // TODO: Do something with the prefix.
        return completionResult.getProposals().stream().map(p -> proposalToElement(p, offset)).toArray(org.eclipse.jface.text.contentassist.ICompletionProposal[]::new);
    }

    /** Gets a resource key from the resource in the editor. */
    private ResourceKey getResourceKeyFromEditor() {
        return new EclipseResourcePath(this.editor.getEditorInput().getAdapter(IResource.class));
    }

    @Override
    public @Nullable IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override
    public @Nullable char[] getCompletionProposalAutoActivationCharacters() {
        // TODO: Make these configurable. Which characters should activate code completion? That's language specific.
        return new char[] { '.' };
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return new char[0];
    }

    @Override
    public @Nullable String getErrorMessage() {
        return this.lastErrorMessage;
    }

    @Override
    public @Nullable IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    private ICompletionProposal proposalToElement(CompletionProposal proposal, int offset) {
        // TODO: Support multiple text edits
        TextEdit primaryTextEdit = !proposal.getEdits().isEmpty() ?proposal.getEdits().get(0) : new TextEdit(Region.atOffset(offset), proposal.getLabel());
        return new org.eclipse.jface.text.contentassist.CompletionProposal(
            primaryTextEdit.getNewText(),
            primaryTextEdit.getRegion().getStartOffset(),
            primaryTextEdit.getRegion().getLength(),
            // Compute where the caret should end up after completing
            // TODO: The caret should be placed in the first placeholder
            primaryTextEdit.getNewText().length(),
            // TODO: Get a good icon for this proposal
            null,
            proposal.getLabel(),
            new ContextInformation(null, "Context display string", "Info display string"),
            proposal.getDescription()
        );
    }
}
