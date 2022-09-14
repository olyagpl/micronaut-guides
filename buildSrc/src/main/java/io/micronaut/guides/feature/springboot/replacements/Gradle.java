/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.guides.feature.springboot.replacements;

import com.fizzed.rocker.RockerModel;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.guides.feature.springboot.SpringBootApplicationFeature;
import io.micronaut.guides.feature.springboot.template.help;
import io.micronaut.guides.feature.springboot.template.sourceCompatibility;
import io.micronaut.guides.feature.springboot.template.springBootBuildGradle;
import io.micronaut.guides.feature.springboot.template.springBootGitignore;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.application.generator.GeneratorContext;
import io.micronaut.starter.build.Property;
import io.micronaut.starter.build.gradle.GradleBuild;
import io.micronaut.starter.build.gradle.GradleBuildCreator;
import io.micronaut.starter.build.gradle.GradlePlugin;
import io.micronaut.starter.feature.Feature;
import io.micronaut.starter.feature.FeatureContext;
import io.micronaut.starter.feature.MicronautRuntimeFeature;
import io.micronaut.starter.feature.build.KotlinBuildPlugins;
import io.micronaut.starter.feature.build.MicronautBuildPlugin;
import io.micronaut.starter.feature.build.gitignore;
import io.micronaut.starter.feature.build.gradle.templates.buildGradle;
import io.micronaut.starter.feature.build.gradle.templates.gradleProperties;
import io.micronaut.starter.feature.build.gradle.templates.settingsGradle;
import io.micronaut.starter.options.BuildTool;
import io.micronaut.starter.options.Options;
import io.micronaut.starter.template.BinaryTemplate;
import io.micronaut.starter.template.RockerTemplate;
import io.micronaut.starter.template.Template;
import io.micronaut.starter.template.URLTemplate;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.micronaut.starter.build.Repository.micronautRepositories;

@Singleton
@Replaces(io.micronaut.starter.feature.build.gradle.Gradle.class)
public class Gradle extends io.micronaut.starter.feature.build.gradle.Gradle {
    protected static final GradlePlugin GROOVY_GRADLE_PLUGIN = GradlePlugin.builder().id("groovy").build();

    protected static final String WRAPPER_JAR = "gradle/wrapper/gradle-wrapper.jar";
    protected static final String WRAPPER_PROPS = "gradle/wrapper/gradle-wrapper.properties";

    protected final KotlinBuildPlugins kotlinBuildPlugins;
    protected final GradleBuildCreator dependencyResolver;
    protected final MicronautBuildPlugin micronautBuildPlugin;

    public Gradle(GradleBuildCreator dependencyResolver,
                  MicronautBuildPlugin micronautBuildPlugin,
                  KotlinBuildPlugins kotlinBuildPlugins) {
        super(dependencyResolver, micronautBuildPlugin, kotlinBuildPlugins);
        this.dependencyResolver = dependencyResolver;
        this.micronautBuildPlugin = micronautBuildPlugin;
        this.kotlinBuildPlugins = kotlinBuildPlugins;
    }

    @Override
    public void processSelectedFeatures(FeatureContext featureContext) {
        if (!SpringBootApplicationFeature.isSpringBootApplication(featureContext)) {
            featureContext.addFeature(micronautBuildPlugin);
            if (kotlinBuildPlugins.shouldApply(featureContext)) {
                featureContext.addFeature(kotlinBuildPlugins);
            }
        }
    }

    @Override
    public void apply(GeneratorContext generatorContext) {
        addGradleInitFiles(generatorContext);
        extraPlugins(generatorContext).forEach(generatorContext::addBuildPlugin);
        GradleBuild build = createBuild(generatorContext);
        addBuildFile(generatorContext, build);
        addGitignore(generatorContext);
        addGradleProperties(generatorContext);
        addSettingsFile(generatorContext, build);
        if (SpringBootApplicationFeature.isSpringBootApplication(generatorContext)) {
            generatorContext.addTemplate("help", new RockerTemplate(Template.ROOT, "HELP.md", help.template()));
        }
    }

    protected GradleBuild createBuild(GeneratorContext generatorContext) {
        return dependencyResolver.create(generatorContext, micronautRepositories());
    }

    protected void addBuildFile(GeneratorContext generatorContext, GradleBuild build) {
        generatorContext.addTemplate("build",
                new RockerTemplate(generatorContext.getBuildTool().getBuildFileName(), buildFile(generatorContext, build)));
    }

    protected RockerModel buildFile(GeneratorContext generatorContext, GradleBuild build) {
        if (SpringBootApplicationFeature.isSpringBootApplication(generatorContext)) {
            return springBootBuildGradle.template(generatorContext.getProject(), build, generatorContext.getFeatures());
        }
        return buildGradle.template(
                generatorContext.getApplicationType(),
                generatorContext.getProject(),
                generatorContext.getFeatures(),
                build
        );
    }

    protected void addGitignore(GeneratorContext generatorContext) {
        generatorContext.addTemplate("gitignore", new RockerTemplate(Template.ROOT, ".gitignore", gitignore(generatorContext)));
    }

    protected RockerModel gitignore(GeneratorContext generatorContext) {
        if (SpringBootApplicationFeature.isSpringBootApplication(generatorContext)) {
            return springBootGitignore.template();
        }
        return gitignore.template();
    }

    @NonNull
    protected static List<Property> gradleProperties(@NonNull GeneratorContext generatorContext) {
        return generatorContext.getBuildProperties().getProperties().stream()
                .filter(p -> p.getKey() == null || !p.getKey().equals(MicronautRuntimeFeature.PROPERTY_MICRONAUT_RUNTIME)) // It is set via the DSL
                .collect(Collectors.toList());
    }

    @Override
    public boolean shouldApply(ApplicationType applicationType,
                               Options options,
                               Set<Feature> selectedFeatures) {
        return options.getBuildTool().isGradle();
    }

    protected void addGradleProperties(GeneratorContext generatorContext) {
        if (!SpringBootApplicationFeature.isSpringBootApplication(generatorContext)) {
            generatorContext.addTemplate("projectProperties", new RockerTemplate(Template.ROOT, "gradle.properties", gradleProperties.template(gradleProperties(generatorContext))));
        }
    }

    protected void addSettingsFile(GeneratorContext generatorContext, GradleBuild build) {
        String settingsFile =  generatorContext.getBuildTool() == BuildTool.GRADLE ? "settings.gradle" : "settings.gradle.kts";
        generatorContext.addTemplate("gradleSettings", new RockerTemplate(Template.ROOT, settingsFile, settingsGradle.template(generatorContext.getProject(), build, generatorContext.getModuleNames())));
    }

    protected List<GradlePlugin> extraPlugins(GeneratorContext generatorContext) {
        if (SpringBootApplicationFeature.isSpringBootApplication(generatorContext)) {
            List<GradlePlugin> result = new ArrayList<>();
            GradlePlugin.Builder builder = null;
            if (generatorContext.getFeatures().language().isGroovy()) {
                builder =  GradlePlugin.builder().id("groovy");
            } else if (generatorContext.getFeatures().language().isJava()) {
                builder =  GradlePlugin.builder().id("java");
            }
            if (builder != null) {
                result.add(builder
                        .extension(new RockerTemplate(sourceCompatibility.template(generatorContext.getFeatures().getTargetJdk())))
                        .build());
            }
            return result;
        } else {
            if (generatorContext.getFeatures().language().isGroovy() || generatorContext.getFeatures().testFramework().isSpock()) {
                return Collections.singletonList(GROOVY_GRADLE_PLUGIN);
            }
            return Collections.emptyList();
        }

    }

    protected void addGradleInitFiles(GeneratorContext generatorContext) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        generatorContext.addTemplate("gradleWrapperJar", new BinaryTemplate(Template.ROOT, WRAPPER_JAR, classLoader.getResource(WRAPPER_JAR)));
        generatorContext.addTemplate("gradleWrapperProperties", new URLTemplate(Template.ROOT, WRAPPER_PROPS, classLoader.getResource(WRAPPER_PROPS)));
        generatorContext.addTemplate("gradleWrapper", new URLTemplate(Template.ROOT, "gradlew", classLoader.getResource("gradle/gradlew"), true));
        generatorContext.addTemplate("gradleWrapperBat", new URLTemplate(Template.ROOT, "gradlew.bat", classLoader.getResource("gradle/gradlew.bat"), false));
    }
}
