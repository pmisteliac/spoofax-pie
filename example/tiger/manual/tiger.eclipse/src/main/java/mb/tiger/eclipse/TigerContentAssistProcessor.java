package mb.tiger.eclipse;

import mb.pie.api.PieSession;
import mb.spoofax.core.language.LanguageInstance;
import mb.spoofax.eclipse.editor.SpoofaxContentAssistProcessor;
import mb.spoofax.eclipse.editor.SpoofaxEditor;

import javax.inject.Inject;
import javax.inject.Provider;

public class TigerContentAssistProcessor extends SpoofaxContentAssistProcessor {

    public static class Factory implements SpoofaxContentAssistProcessor.Factory {
        LanguageInstance languageInstance;
        Provider<PieSession> pieSessionProvider;
        @Inject public Factory(
            LanguageInstance languageInstance,
            Provider<PieSession> pieSessionProvider
        ) {
            this.languageInstance = languageInstance;
            this.pieSessionProvider = pieSessionProvider;
        }
        @Override
        public TigerContentAssistProcessor create(SpoofaxEditor editor) {
            return new TigerContentAssistProcessor(editor, languageInstance, pieSessionProvider);
        }
    }

    protected TigerContentAssistProcessor(
        SpoofaxEditor editor,
        LanguageInstance languageInstance,
        Provider<PieSession> pieSessionProvider
    ) {
        super(editor, languageInstance, pieSessionProvider);
    }
}
