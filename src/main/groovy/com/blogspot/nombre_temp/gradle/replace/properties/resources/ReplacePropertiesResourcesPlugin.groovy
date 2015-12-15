package com.blogspot.nombre_temp.gradle.replace.properties.resources

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This Gradle plugin replaces the values in ".properties" files inside the project's resources with values from files with the same name inside a different
 * name after the [processResources] Gradle task, which is used during the build process for projects with code to be executed in the [JVM] (like Java and Groovy).
 */
class ReplacePropertiesResourcesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {

        project.afterEvaluate { p ->
            def processResourcesTask = p.tasks.findByName('processResources')

            if (processResourcesTask == null) {
                println "processResources task not found. Make sure to apply the plugin that has it before (for example 'java' or 'groovy')."
            } else {
                processResourcesTask.inputs.properties System.properties
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
                    processResourcesTask.inputs.dir p.file("$configEnvironmentFolder/$environment")

                    processResourcesTask.doLast {
                        println "***********************************************************"
                        println "Using environment: $environment"
                        println "***********************************************************"

                        p.fileTree(dir: "$configEnvironmentFolder/$environment" , include: '**/*.properties').each { file ->
                            def fileRelativePath = environmentFolder.toURI().relativize( file.toURI() ).path

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
}
