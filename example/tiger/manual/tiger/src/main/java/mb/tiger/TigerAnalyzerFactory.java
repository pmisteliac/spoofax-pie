package mb.tiger;

import mb.log.api.LoggerFactory;
import mb.spoofax.compiler.interfaces.spoofaxcore.ParserFactory;
import mb.statix.spec.Spec;
import org.spoofax.interpreter.terms.ITermFactory;

import javax.inject.Inject;

public class TigerAnalyzerFactory {
    private final TigerStatixSpec spec;
    private ITermFactory termFactory;
    private LoggerFactory loggerFactory;

    @Inject public TigerAnalyzerFactory(
        ITermFactory termFactory,
        LoggerFactory loggerFactory
    ) {
        this.termFactory = termFactory;
        this.loggerFactory = loggerFactory;
        this.spec = TigerStatixSpec.fromClassLoaderResources();
    }

    public TigerAnalyzer create() {
        return new TigerAnalyzer(spec, termFactory, loggerFactory);
    }
}
