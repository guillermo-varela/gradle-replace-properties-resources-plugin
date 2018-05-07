package com.blogspot.nombre_temp.gradle.replace.properties.resources

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * This Gradle plugin replaces the values in ".properties" files inside the project's resources with values from files with the same name inside a different
 * name after the "processResources" Gradle task, which is used during the build process for projects with code to be executed in the JVM (like Java and Groovy).
 *
 * For files other than ".properties" they will also be copied and filtered using using the ReplaceTokens feature from Ant.
 * Any text between "@" symbols (tokens) will be replaced with a value from a system property with the same key as the surrounded text.
 */
class ReplacePropertiesResourcesPlugin implements Plugin<Project> {

    private static final String processResourcesTaskName = 'processResources'
    private static final String processTestResourcesTaskName = 'processTestResources'

    @Override
    void apply(Project project) {

        project.afterEvaluate { p ->
            def processResourcesTask = p.tasks.findByName(processResourcesTaskName)
            def processTestResourcesTask = p.tasks.findByName(processTestResourcesTaskName)

            updatePropertiesFileWithEnvironment(processResourcesTask, p, processResourcesTaskName)
            updatePropertiesFileWithEnvironment(processTestResourcesTask, p, processTestResourcesTaskName)
        }
    }

    private void updatePropertiesFileWithEnvironment(Task resourceTask, Project p, String taskName) {
        if (resourceTask == null) {
            println "$taskName task not found. Make sure to apply the plugin that has it before (for example 'java' or 'groovy')."
        } else {
            resourceTask.inputs.properties System.properties
            def environment = System.properties.env

            if (environment != null) {
                def configEnvironmentFolder = p.properties.configEnvironmentFolder ? p.properties.configEnvironmentFolder : 'config'
                def environmentFolder = p.file("$configEnvironmentFolder/$environment")

                if (!p.file(configEnvironmentFolder).exists()) {
                    throw new InvalidUserDataException("Configuration environment folder not found: $configEnvironmentFolder")
                }
                if (!environmentFolder.exists()) {
                    throw new InvalidUserDataException("Environment folder not found: $configEnvironmentFolder/$environment")
                }

                // Executed only if the configuration files or the system properties changed from previous execution
                resourceTask.inputs.dir p.file("$configEnvironmentFolder/$environment")

                resourceTask.doLast {
                    println "***********************************************************"
                    println "Using environment: $environment"
                    println "***********************************************************"

                    // Copy all resources files, except ".properties"
                    p.fileTree(dir: "$configEnvironmentFolder/$environment", exclude: '**/*.properties').each { file ->
                        def fileRelativePath = environmentFolder.toURI().relativize(file.toURI()).path

                        p.sourceSets.each { source ->
                            // Gets the corresponding file in the resources build folder
                            def ouputFile = p.file("$source.output.resourcesDir/$fileRelativePath")
                            if (ouputFile.exists()) {
                                p.copy {
                                    into ouputFile.parent
                                    from(file) {
                                        filter(ReplaceTokens, tokens: System.properties)
                                    }
                                }
                            }
                        }
                    }

                    p.fileTree(dir: "$configEnvironmentFolder/$environment", include: '**/*.properties').each { file ->
                        def fileRelativePath = environmentFolder.toURI().relativize(file.toURI()).path

                        p.sourceSets.each { source ->
                            // Gets the corresponding file in the resources build folder
                            def ouputFile = p.file("$source.output.resourcesDir/$fileRelativePath")
                            if (ouputFile.exists()) {
                                def environmentProperties = new Properties()

                                file.withInputStream {
                                    environmentProperties.load(it);
                                }

                                // Overwrites the values in the file with the ones given from command line arguments -Dkey
                                System.properties.each { key, value ->
                                    if (environmentProperties.containsKey(key)) {
                                        environmentProperties.put(key, value)
                                    }
                                }

                                // Replaces all values on "ouputFile" with the ones contained in "environmentProperties"
                                environmentProperties.each { propKey, propValue ->
                                    p.ant.propertyfile(file: ouputFile) {
                                        entry(key: propKey, type: 'string', operation: '=', value: propValue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
