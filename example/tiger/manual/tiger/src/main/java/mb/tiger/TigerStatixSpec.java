package mb.tiger;

import com.google.common.collect.ListMultimap;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderOriginTermFactory;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.binary.TermReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
//
//public class TigerStatixSpec implements Serializable {
//    final Spec spec;
//
//    private TigerStatixSpec(Spec spec) {
//        this.spec = spec;
//    }
//
//    public static TigerStatixSpec fromClassLoaderResources() {
//        // NOTE: This aterm is of the form Spec(..), which you get by running
//        // the Generate > Generate Combined ATerm builder on a Statix .stx specification.
//        // Currently, its name must be strc, and its root rule must be `programOK(e)`.
//        final String resource = "/mb/tiger/statix.aterm";
////        final String resource = "mb/tiger/target/metaborg/statix.aterm";
//        try(final @Nullable InputStream inputStream = TigerStatixSpec.class.getClassLoader().getResourceAsStream(resource)) {
//            if(inputStream == null) {
//                throw new RuntimeException("Cannot create Spec; cannot find resource '" + resource + "' in classloader resources");
//            }
//            final ITermFactory termFactory = new ImploderOriginTermFactory(new TermFactory());
//            final @Nullable Spec spec = getSpec(fromStream(inputStream, termFactory), termFactory);
//            if (spec == null) {
//                throw new RuntimeException("Cannot create Spec; resource '" + resource + "' in classloader resources does not conform to expected format.");
//            }
//            return new TigerStatixSpec(spec);
//        } catch(IOException | InterpreterException e) {
//            throw new RuntimeException("Cannot create Spec; cannot read parse table from resource '" + resource + "' in classloader resources", e);
//        }
//    }
//
//    public static IStrategoTerm fromStream(InputStream parseTableStream, ITermFactory termFactory) {
//        final TermReader reader = new TermReader(termFactory);
//        try {
//            return reader.parseFromStream(parseTableStream);
//        } catch(IOException e) {
//            throw new IllegalStateException("Loading ATerm from stream failed unexpectedly", e);
//        }
//    }
//
//    /**
//     * Gets the Statix specification from the specified term.
//     *
//     * @param specAst the specification term
//     * @param termFactory the term factory
//     * @return the specification; or {@code null} when it is invalid
//     */
//    public static @Nullable Spec getSpec(IStrategoTerm specAst, ITermFactory termFactory) throws InterpreterException {
//        final ITerm specTerm = new StrategoTerms(termFactory).fromStratego(specAst);
//        final Spec spec = StatixTerms.spec().match(specTerm).orElseThrow(() -> new InterpreterException("Expected spec, got " + specTerm));
//        if (!checkNoOverlappingRules(spec)) {
//            // Invalid specification.
//            return null;
//        }
//        return spec;
//    }
//
//    /**
//     * Reports any overlapping rules in the specification.
//     *
//     * @param spec the specification to check
//     * @return {@code true} when the specification has no overlapping rules;
//     * otherwise, {@code false}.
//     */
//    private static boolean checkNoOverlappingRules(Spec spec) {
//        final ListMultimap<String, Rule> rulesWithEquivalentPatterns = spec.rules().getAllEquivalentRules();
//        if(!rulesWithEquivalentPatterns.isEmpty()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("Found rules with equivalent patterns.\n");
//            for(Map.Entry<String, Collection<Rule>> entry : rulesWithEquivalentPatterns.asMap().entrySet()) {
//                sb.append("Overlapping rules for: ").append(entry.getKey()).append("\n");
//                for(Rule rule : entry.getValue()) {
//                    sb.append("* ").append(rule).append("\n");
//                }
//            }
//            throw new IllegalStateException(sb.toString());
//        }
//        return rulesWithEquivalentPatterns.isEmpty();
//    }
//}
