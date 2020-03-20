import mb.common.util.ListView
import mb.spoofax.compiler.cli.CliCommandRepr
import mb.spoofax.compiler.cli.CliParamRepr
import mb.spoofax.compiler.command.ArgProviderRepr
import mb.spoofax.compiler.command.AutoCommandRequestRepr
import mb.spoofax.compiler.command.CommandDefRepr
import mb.spoofax.compiler.command.ParamRepr
import mb.spoofax.compiler.gradle.spoofaxcore.AdapterProjectCompilerSettings
import mb.spoofax.compiler.menu.CommandActionRepr
import mb.spoofax.compiler.menu.MenuItemRepr
import mb.spoofax.compiler.spoofaxcore.AdapterProjectCompiler
import mb.spoofax.compiler.spoofaxcore.CompleterCompiler
import mb.spoofax.compiler.spoofaxcore.ConstraintAnalyzerCompiler
import mb.spoofax.compiler.spoofaxcore.ParserCompiler
import mb.spoofax.compiler.spoofaxcore.StrategoRuntimeCompiler
import mb.spoofax.compiler.spoofaxcore.StylerCompiler
import mb.spoofax.compiler.util.StringUtil
import mb.spoofax.compiler.util.TypeInfo
import mb.spoofax.core.language.command.CommandContextType
import mb.spoofax.core.language.command.CommandExecutionType
import mb.spoofax.core.language.command.HierarchicalResourceType
import java.util.Optional

plugins {
  id("org.metaborg.spoofax.compiler.gradle.spoofaxcore.adapter")
}

adapterProjectCompiler {
  settings.set(AdapterProjectCompilerSettings(
    parser = ParserCompiler.AdapterProjectInput.builder(),
    styler = StylerCompiler.AdapterProjectInput.builder(),
    completer = CompleterCompiler.AdapterProjectInput.builder(),
    strategoRuntime = StrategoRuntimeCompiler.AdapterProjectInput.builder(),
    constraintAnalyzer = ConstraintAnalyzerCompiler.AdapterProjectInput.builder(),
    compiler = run {
      val taskPackageId = "mb.tiger.spoofax.task"
      val commandPackageId = "mb.tiger.spoofax.command"

      val builder = AdapterProjectCompiler.Input.builder();

      // Show parsed/desugar/analyzed/pretty-printed tasks and commands
      val showArgs = TypeInfo.of(taskPackageId, "TigerShowArgs")
      val showParams = listOf(
        ParamRepr.of("resource", TypeInfo.of("mb.resource", "ResourceKey"), true, ArgProviderRepr.context(CommandContextType.ResourceKey)),
        ParamRepr.of("region", TypeInfo.of("mb.common.region", "Region"), false, ArgProviderRepr.context(CommandContextType.Region))
      )

      val showPrettyPrintedText = TypeInfo.of(taskPackageId, "TigerShowPrettyPrintedText")
      builder.addTaskDefs(showPrettyPrintedText)
      val showPrettyPrintedTextCommand = CommandDefRepr.builder()
        .type(commandPackageId, "TigerShowPrettyPrintedTextCommand")
        .taskDefType(showPrettyPrintedText)
        .argType(showArgs)
        .displayName("Show pretty-printed text")
        .description("Shows a pretty-printed version of the program")
        .addSupportedExecutionTypes(CommandExecutionType.ManualOnce, CommandExecutionType.ManualContinuous)
        .addAllParams(showParams)
        .build()
      builder.addCommandDefs(showPrettyPrintedTextCommand)

      // Menu bindings
      val mainAndEditorMenu = listOf(CommandActionRepr.builder().manualOnce(showPrettyPrintedTextCommand).buildItem())
      builder.addAllMainMenuItems(mainAndEditorMenu)
      builder.addAllEditorContextMenuItems(mainAndEditorMenu)
    }
  ))
}
