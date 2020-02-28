package mb.tiger.eclipse;

import dagger.Module;
import dagger.Provides;
import mb.pie.api.PieSession;
import mb.spoofax.core.language.LanguageInstance;
import mb.spoofax.core.language.LanguageScope;
import mb.spoofax.eclipse.EclipseIdentifiers;
import mb.spoofax.eclipse.editor.SpoofaxContentAssistProcessor;
import mb.spoofax.eclipse.editor.SpoofaxEditor;
import mb.spoofax.eclipse.editor.SpoofaxSourceViewerConfiguration;

import javax.inject.Provider;

@Module
public class TigerEclipseModule {
    @Provides @LanguageScope
    EclipseIdentifiers provideEclipseIdentifiers() {
        return new TigerEclipseIdentifiers();
    }

    @Provides
    SpoofaxContentAssistProcessor.Factory provideContentAssistProcessorFactory(
        LanguageInstance languageInstance,
        Provider<PieSession> pieSessionProvider
    ) {
        return new TigerContentAssistProcessor.Factory(languageInstance, pieSessionProvider);
    }
}
