package mb.tiger.spoofax.util;

import dagger.Component;
import mb.pie.dagger.PieModule;
import mb.resource.text.TextResourceRegistry;
import mb.spoofax.core.platform.LoggerFactoryModule;
import mb.spoofax.core.platform.PlatformComponent;
import mb.spoofax.core.platform.ResourceRegistriesModule;
import mb.spoofax.core.platform.ResourceServiceModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
    LoggerFactoryModule.class,
    ResourceRegistriesModule.class,
    ResourceServiceModule.class,
    PieModule.class
})
public interface PlatformTestComponent extends PlatformComponent {
    TextResourceRegistry getTextResourceRegistry();
}
