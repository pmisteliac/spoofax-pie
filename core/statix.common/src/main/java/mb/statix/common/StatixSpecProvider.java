package mb.statix.common;

import com.google.common.collect.ListMultimap;
import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

public class StatixSpecProvider {

    private final Logger log;
    private final StrategoTerms strategoTerms;
    private final ITermFactory termFactory;

    @Inject public StatixSpecProvider(
        LoggerFactory loggerFactory,
        ITermFactory termFactory
    ) {
        this.log = loggerFactory.create(getClass());
        this.strategoTerms = new StrategoTerms(termFactory);
        this.termFactory = termFactory;
    }

    /**
     * Gets the Statix specification from the specified term.
     *
     * @param specAst the specification term
     * @return the specification; or {@code null} when it is invalid
     */
    public @Nullable Spec getSpec(IStrategoTerm specAst) throws InterpreterException {
        final ITerm specTerm = strategoTerms.fromStratego(specAst);
        final Spec spec = StatixTerms.spec().match(specTerm).orElseThrow(() -> new InterpreterException("Expected spec, got " + specTerm));
        if (!checkNoOverlappingRules(spec)) {
            // Invalid specification.
            return null;
        }
        return spec;
    }

    /**
     * Reports any overlapping rules in the specification.
     *
     * @param spec the specification to check
     * @return {@code true} when the specification has no overlapping rules;
     * otherwise, {@code false}.
     */
    private boolean checkNoOverlappingRules(Spec spec) {
        final ListMultimap<String, Rule> rulesWithEquivalentPatterns = spec.rules().getAllEquivalentRules();
        if(!rulesWithEquivalentPatterns.isEmpty()) {
            log.error("+--------------------------------------+");
            log.error("| FOUND RULES WITH EQUIVALENT PATTERNS |");
            log.error("+--------------------------------------+");
            for(Map.Entry<String, Collection<Rule>> entry : rulesWithEquivalentPatterns.asMap().entrySet()) {
                log.error("| Overlapping rules for: {}", entry.getKey());
                for(Rule rule : entry.getValue()) {
                    log.error("| * {}", rule);
                }
            }
            log.error("+--------------------------------------+");
        }
        return rulesWithEquivalentPatterns.isEmpty();
    }
}
