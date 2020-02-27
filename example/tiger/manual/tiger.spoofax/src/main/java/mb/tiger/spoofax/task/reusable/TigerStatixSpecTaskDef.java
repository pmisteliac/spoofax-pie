package mb.tiger.spoofax.task.reusable;

import mb.pie.api.ExecContext;
import mb.pie.api.Supplier;
import mb.pie.api.TaskDef;
import mb.spoofax.core.language.LanguageScope;
import mb.statix.common.StatixSpecProvider;
import mb.statix.spec.Spec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.terms.IStrategoTerm;

import javax.inject.Inject;
import java.io.Serializable;

@LanguageScope
public class TigerStatixSpecTaskDef implements TaskDef<TigerStatixSpecTaskDef.Input, @Nullable Spec> {

    public static class Input implements Serializable {
        public final Supplier<@Nullable IStrategoTerm> specAstSupplier;

        public Input(Supplier<@Nullable IStrategoTerm> specAstSupplier) {
            this.specAstSupplier = specAstSupplier;
        }
    }

    private final StatixSpecProvider specProvider;

    @Inject public TigerStatixSpecTaskDef(
        StatixSpecProvider specProvider
    ) {
        this.specProvider = specProvider;
    }

    @Override
    public String getId() {
        return TigerStatixSpecTaskDef.class.getName();
    }

    @Override
    public @Nullable Spec exec(ExecContext context, Input input) throws Exception {
        @Nullable IStrategoTerm specAst = input.specAstSupplier.get(context);
        if (specAst == null) return null;
        return specProvider.getSpec(specAst);
    }
}
