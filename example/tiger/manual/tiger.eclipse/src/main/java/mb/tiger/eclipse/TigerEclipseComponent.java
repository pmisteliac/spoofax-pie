package mb.tiger.eclipse;

import dagger.Component;
import mb.spoofax.core.language.LanguageScope;
import mb.spoofax.core.platform.PlatformComponent;
import mb.spoofax.eclipse.EclipseLanguageComponent;
import mb.spoofax.eclipse.editor.SpoofaxSourceViewerConfiguration;
import mb.tiger.spoofax.SpoofaxModule;
import mb.tiger.spoofax.TigerComponent;
import mb.tiger.spoofax.TigerModule;

@LanguageScope
@Component(modules = {
    TigerModule.class,
    TigerEclipseModule.class,
    SpoofaxModule.class
}, dependencies = PlatformComponent.class)
public interface TigerEclipseComponent extends EclipseLanguageComponent, TigerComponent {
    TigerEditorTracker getEditorTracker();
}
