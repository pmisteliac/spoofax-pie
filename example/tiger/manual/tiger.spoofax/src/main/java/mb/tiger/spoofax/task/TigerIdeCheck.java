package mb.tiger.spoofax.task;

import mb.common.message.Messages;
import mb.common.message.MessagesBuilder;
import mb.jsglr1.common.JSGLR1ParseResult;
import mb.pie.api.ExecContext;
import mb.pie.api.ExecException;
import mb.pie.api.ResourceStringSupplier;
import mb.pie.api.TaskDef;
import mb.resource.ResourceKey;
import mb.spoofax.core.language.LanguageScope;
import mb.tiger.spoofax.task.reusable.TigerAnalyze;
import mb.tiger.spoofax.task.reusable.TigerParse;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.inject.Inject;

@LanguageScope
public class TigerIdeCheck implements TaskDef<ResourceKey, Messages> {
    private final TigerParse parse;
    private final TigerAnalyze analyze;

    @Inject public TigerIdeCheck(TigerParse parse, TigerAnalyze analyze) {
        this.parse = parse;
        this.analyze = analyze;
    }

    @Override public String getId() {
        return "mb.tiger.spoofax.task.TigerCheck";
    }

    @Override
    public Messages exec(ExecContext context, ResourceKey key) throws ExecException, InterruptedException {
        final MessagesBuilder builder = new MessagesBuilder();
        final ResourceStringSupplier stringProvider = new ResourceStringSupplier(key);
        final JSGLR1ParseResult parseResult = context.require(parse, stringProvider);
        builder.addMessages(parseResult.getMessages());
        final TigerAnalyze.@Nullable Output analysisOutput = context.require(analyze, new TigerAnalyze.Input(key, parse.createAstSupplier(stringProvider)));
        if(analysisOutput != null) {
            builder.addMessages(analysisOutput.result.messages);
        }
        return builder.build();
    }
}
