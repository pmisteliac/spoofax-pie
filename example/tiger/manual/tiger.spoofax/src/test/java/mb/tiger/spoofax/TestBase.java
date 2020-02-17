package mb.tiger.spoofax;

import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.log.slf4j.SLF4JLoggerFactory;
import mb.pie.dagger.PieModule;
import mb.pie.runtime.PieBuilderImpl;
import mb.resource.ResourceService;
import mb.resource.text.TextResourceRegistry;
import mb.spoofax.core.platform.LoggerFactoryModule;
import mb.tiger.spoofax.util.DaggerPlatformTestComponent;
import mb.tiger.spoofax.util.DaggerTigerTestComponent;
import mb.tiger.spoofax.util.PlatformTestComponent;
import mb.tiger.spoofax.util.TigerTestComponent;

class TestBase {
    final PlatformTestComponent platformComponent = DaggerPlatformTestComponent
        .builder()
        .loggerFactoryModule(new LoggerFactoryModule(new SLF4JLoggerFactory()))
        .pieModule(new PieModule(PieBuilderImpl::new))
        .build();
    final LoggerFactory loggerFactory = platformComponent.getLoggerFactory();
    final Logger log = loggerFactory.create(TestBase.class);
    final ResourceService resourceService = platformComponent.getResourceService();
    final TextResourceRegistry textResourceRegistry = platformComponent.getTextResourceRegistry();

    final TigerTestComponent languageComponent = DaggerTigerTestComponent
        .builder()
        .platformComponent(platformComponent)
        .tigerModule(new TigerModule())
        .build();
    final TigerInstance languageInstance = languageComponent.getLanguageInstance();
}
