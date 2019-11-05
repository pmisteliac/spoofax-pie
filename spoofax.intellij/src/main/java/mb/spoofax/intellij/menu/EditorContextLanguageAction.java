package mb.spoofax.intellij.menu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import mb.common.region.Selection;
import mb.common.util.ListView;
import mb.pie.api.ExecException;
import mb.pie.api.PieSession;
import mb.pie.api.exec.NullCancelableToken;
import mb.resource.ResourceService;
import mb.spoofax.core.language.command.CommandContext;
import mb.spoofax.core.language.command.CommandExecutionType;
import mb.spoofax.core.language.command.CommandRequest;
import mb.spoofax.intellij.IntellijLanguageComponent;
import mb.spoofax.intellij.editor.EditorUtils;
import mb.spoofax.intellij.pie.PieRunner;
import mb.spoofax.intellij.resource.IntellijResource;
import mb.spoofax.intellij.resource.IntellijResourceRegistry;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.swing.*;


/**
 * A language action executed in the context of an editor.
 */
public abstract class EditorContextLanguageAction extends LanguageAction {

    private final CommandRequest commandRequest;

    private final IntellijLanguageComponent languageComponent;
    private final IntellijResourceRegistry resourceRegistry;
    private final PieRunner pieRunner;


    /**
     * Factory interface for the {@link EditorContextLanguageAction} class.
     */
    public interface Factory {
        EditorContextLanguageAction create(
                String id,
                CommandRequest commandRequest,
                @Nullable String text,
                @Nullable String description,
                @Nullable Icon icon);
    }
//
//    /**
//     * Factory class for the {@link EditorContextLanguageAction} class.
//     */
//    public static final class Factory {
//        private final IntellijLanguageComponent languageComponent;
//        private final IntellijResourceRegistry resourceRegistry;
//        private final PieRunner pieRunner;
//
//        @Inject
//        public Factory(IntellijLanguageComponent languageComponent,
//                       IntellijResourceRegistry resourceRegistry,
//                       PieRunner pieRunner) {
//            this.languageComponent = SpoofaxPlugin.getComponent();
//            this.resourceRegistry = resourceRegistry;
//            this.pieRunner = pieRunner;
//        }
//
//        EditorContextLanguageAction create(
//                String id,
//                CommandRequest commandRequest,
//                @Nullable String text,
//                @Nullable String description,
//                @Nullable Icon icon) {
//            return new EditorContextLanguageAction(id, commandRequest, text, description, icon,
//                    this.languageComponent, this.resourceRegistry, this.pieRunner);
//        }
//    }

    /**
     * Initializes a new instance of the {@link AnActionWithId} class.
     *
     * @param id          The ID of the action.
     * @param text        The text of the action; or <code>null</code>.
     * @param description The description of the action; or <code>null</code>.
     * @param icon        The icon of the action; or <code>null</code>.
     */
    public EditorContextLanguageAction(
            String id, CommandRequest commandRequest,
            @Nullable String text, @Nullable String description, @Nullable Icon icon,
            IntellijLanguageComponent languageComponent,
            IntellijResourceRegistry resourceRegistry,
            PieRunner pieRunner) {
        super(id, text, description, icon);
        this.commandRequest = commandRequest;
        this.languageComponent = languageComponent;
        this.resourceRegistry = resourceRegistry;
        this.pieRunner = pieRunner;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (this.commandRequest.executionType == CommandExecutionType.AutomaticContinuous) {
            return; // Automatic continuous execution is not supported when manually invoking commands.
        }

        @Nullable Project project = e.getProject();
        if (project == null) {
            // The action needs a project.
            return;
        }

        @Nullable IntellijResource resource = this.resourceRegistry.getResource(e);
        @Nullable Editor editor = ActionUtils.getEditor(e);
        if (resource == null || editor == null) return;

        Selection selection = EditorUtils.getPrimarySelection(editor);
        CommandContext context = new CommandContext(resource.getKey(), selection);
        if (!context.isSupportedBy(this.commandRequest.def.getRequiredContextTypes())) {
            return; // Context is not supported by command.
        }
        try {
            try(final PieSession session = this.languageComponent.newPieSession()) {
                this.pieRunner.requireCommand(this.languageComponent, this.commandRequest, project, ListView.of(context), session, NullCancelableToken.instance);
            }
        } catch(ExecException ex) {
            throw new RuntimeException("Cannot execute command request '" + this.commandRequest + "', execution failed unexpectedly", ex);
        } catch(InterruptedException ex) {
            // Ignore
        }
    }

}