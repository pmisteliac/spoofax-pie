package mb.tiger.spoofax;

import dagger.Module;
import dagger.Provides;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.spoofax.core.language.LanguageScope;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderOriginTermFactory;
import org.spoofax.terms.TermFactory;

@Module
public class SpoofaxModule {

    @Provides @LanguageScope
    ITermFactory provideTermFactory() {
        return new ImploderOriginTermFactory(new TermFactory());
    }

    @Provides @LanguageScope
    StrategoTerms provideStrategoTerms(ITermFactory termFactory) {
        return new StrategoTerms(termFactory);
    }

}
