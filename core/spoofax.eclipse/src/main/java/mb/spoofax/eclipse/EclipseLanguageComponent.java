package mb.spoofax.eclipse;

import mb.spoofax.core.language.LanguageComponent;
import mb.spoofax.core.language.LanguageScope;
import mb.spoofax.eclipse.editor.SpoofaxContentAssistProcessor;
import mb.spoofax.eclipse.editor.SpoofaxSourceViewerConfiguration;

@LanguageScope
public interface EclipseLanguageComponent extends LanguageComponent {
    EclipseIdentifiers getEclipseIdentifiers();
    SpoofaxSourceViewerConfiguration.Factory getSourceViewerConfigurationFactory();
    SpoofaxContentAssistProcessor.Factory getContentAssistProcessorFactory();
}
