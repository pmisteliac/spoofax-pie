package mb.spoofax.eclipse.editor;

import mb.spoofax.eclipse.EclipseLanguageComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import javax.inject.Inject;

/**
 * This is used to configure the source viewer used to display a document in a text editor.
 */
public class SpoofaxSourceViewerConfiguration extends TextSourceViewerConfiguration {

    public static class Factory {
        private final SpoofaxContentAssistProcessor.Factory contentAssistProcessorFactory;
        @Inject public Factory(SpoofaxContentAssistProcessor.Factory contentAssistProcessorFactory) {
            this.contentAssistProcessorFactory = contentAssistProcessorFactory;
        }
        public SpoofaxSourceViewerConfiguration create(SpoofaxEditor editor) {
            return new SpoofaxSourceViewerConfiguration(editor, contentAssistProcessorFactory);
        }
    }

    private final SpoofaxEditor editor;
    private final SpoofaxContentAssistProcessor.Factory contentAssistProcessorFactory;

    public SpoofaxSourceViewerConfiguration(SpoofaxEditor editor, SpoofaxContentAssistProcessor.Factory contentAssistProcessorFactory) {
        this.editor = editor;
        this.contentAssistProcessorFactory = contentAssistProcessorFactory;
    }

    @Override public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new DefaultTextHover(sourceViewer);
    }

    @Override public @Nullable IReconciler getReconciler(@NonNull ISourceViewer sourceViewer) {
        // Return null to disable TextSourceViewerConfiguration reconciler which does spell checking.
        return null;
    }

    @Override public @Nullable IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        // Return null to disable TextSourceViewerConfiguration quick assist which does spell checking.
        return null;
    }

    @Override public @Nullable IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        ContentAssistant assistant = new ContentAssistant();
        assistant.setContentAssistProcessor(contentAssistProcessorFactory.create(editor), IDocument.DEFAULT_CONTENT_TYPE);
        // Automatically activate code completion while typing
        assistant.enableAutoActivation(true);
        // Automatically insert the proposal is there is only one
        assistant.enableAutoInsert(true);
        // FIXME: This is not needed?
        //assistant.install(sourceViewer);
        return assistant;
    }
}
