package mb.spoofax.compiler.spoofaxcore;

import com.samskivert.mustache.Template;
import mb.resource.ResourceService;
import mb.resource.hierarchical.HierarchicalResource;
import mb.resource.hierarchical.ResourcePath;
import mb.spoofax.compiler.util.ResourceWriter;
import mb.spoofax.compiler.util.TemplateCompiler;
import org.immutables.value.Value;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Value.Enclosing
public class RootProject {
    private final Template settingsGradleTemplate;
    private final Template buildGradleTemplate;
    private final ResourceService resourceService;
    private final Charset charset;


    private RootProject(Template settingsGradleTemplate, Template buildGradleTemplate, ResourceService resourceService, Charset charset) {
        this.resourceService = resourceService;
        this.charset = charset;
        this.settingsGradleTemplate = settingsGradleTemplate;
        this.buildGradleTemplate = buildGradleTemplate;
    }

    public static RootProject fromClassLoaderResources(ResourceService resourceService, Charset charset) {
        final TemplateCompiler templateCompiler = new TemplateCompiler(RootProject.class);
        return new RootProject(
            templateCompiler.compile("root_project/settings.gradle.kts.mustache"),
            templateCompiler.compile("root_project/build.gradle.kts.mustache"),
            resourceService,
            charset
        );
    }


    public Output compile(Input input) throws IOException {
        final Shared shared = input.shared();
        final HierarchicalResource baseDirectory = resourceService.getHierarchicalResource(shared.rootProject().baseDirectory());
        baseDirectory.ensureDirectoryExists();

        final ArrayList<String> includedProjects = new ArrayList<>(input.additionalIncludedProjects());
        includedProjects.add(shared.languageProject().coordinate().artifactId());
        includedProjects.add(shared.adapterProject().coordinate().artifactId());

        final HierarchicalResource settingsGradleKtsFile = resourceService.getHierarchicalResource(input.settingsGradleKtsFile());
        try(final ResourceWriter writer = new ResourceWriter(settingsGradleKtsFile, charset)) {
            final HashMap<String, Object> map = new HashMap<>();
            map.put("includedProjects", includedProjects);
            settingsGradleTemplate.execute(input, map, writer);
            writer.flush();
        }

        final HierarchicalResource buildGradleKtsFile = resourceService.getHierarchicalResource(input.buildGradleKtsFile());
        try(final ResourceWriter writer = new ResourceWriter(buildGradleKtsFile, charset)) {
            buildGradleTemplate.execute(input, writer);
            writer.flush();
        }

        return Output.builder().fromInput(input).build();
    }


    @Value.Immutable
    public interface Input extends Serializable {
        class Builder extends RootProjectData.Input.Builder {}

        static Builder builder() {
            return new Builder();
        }


        Shared shared();


        @Value.Default default ResourcePath buildGradleKtsFile() {
            return shared().rootProject().baseDirectory().appendRelativePath("build.gradle.kts");
        }

        @Value.Default default ResourcePath settingsGradleKtsFile() {
            return shared().rootProject().baseDirectory().appendRelativePath("settings.gradle.kts");
        }

        List<String> additionalIncludedProjects();
    }

    @Value.Immutable
    public interface Output extends Serializable {
        class Builder extends RootProjectData.Output.Builder {
            public Builder fromInput(Input input) {
                baseDirectory(input.shared().rootProject().baseDirectory());
                buildGradleKtsFile(input.buildGradleKtsFile());
                settingsGradleKtsFile(input.settingsGradleKtsFile());
                return this;
            }
        }

        static Builder builder() {
            return new Builder();
        }


        ResourcePath baseDirectory();

        ResourcePath buildGradleKtsFile();

        ResourcePath settingsGradleKtsFile();
    }
}