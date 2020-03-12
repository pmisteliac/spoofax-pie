package mb.tiger;

import mb.jsglr.common.MoreTermUtils;
import mb.log.api.LoggerFactory;
import mb.spoofax.compiler.interfaces.spoofaxcore.ParserFactory;
import mb.statix.common.StatixSpec;
import mb.statix.spec.Spec;
import org.spoofax.interpreter.terms.ITermFactory;

import javax.inject.Inject;

public class TigerAnalyzerFactory {
    private final StatixSpec spec;
    private ITermFactory termFactory;
    private LoggerFactory loggerFactory;

    @Inject public TigerAnalyzerFactory(
        ITermFactory termFactory,
        LoggerFactory loggerFactory
    ) {
        this.termFactory = termFactory;
        this.loggerFactory = loggerFactory;
        this.spec = StatixSpec.fromClassLoaderResources(TigerAnalyzerFactory.class, "/mb/tiger/statix.aterm");
    }

    public TigerAnalyzer create() {
        return new TigerAnalyzer(spec, termFactory, loggerFactory);
    }
}
