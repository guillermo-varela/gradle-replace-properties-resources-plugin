# gradle-replace-properties-resources-plugin

This plugin for Gradle adds the [PropertyFile] feature from Ant to the [processResources] Gradle task, which is commonly used during the build process for projects with code to be executed in the [JVM] (like Java and Groovy).

It takes the project's resource files (by default in "src/main/resources") and replaces the values in ".properties" files with the ones from ".properties" files (with the same name and location) inside a specific folder in the project o from a system property, which allows to replace the values according to the [deployment environment].

### Example
We can have a project with the following structure:
```sh
|   build.gradle
|   gradle.properties
|
+---config
|   +---prod_1
|   |       application.properties
|   |       server.properties
|   |       logback.xml
|   |
|   +---prod_2
|   |       application.properties
|   |       server.properties
|   |       logback.xml
|   |
|   \---qa
|           application.properties
|           server.properties
|           logback.xml
|
\---src
    \---main
        +---java
        |
        \---resources
                application.properties
                server.properties
                logback.xml
```
By default this plugin will look for the deployment environment folders inside the folder "**config**" on the project's root. Each subfolder (deployment environment) inside can have any name you like and can be as many as you need, the ones used here are just an example.

Let's say we have this on:
"**src/main/resources/application.properties**"
```sh
app.instance.name=Jetty-Server
app.instance.number=1
```
"**src/main/resources/config.json**"
```json
{"config": "test"}
```
"**src/main/resources/logback.xml**"
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <property name="LOG_PATH" value="/logs/" />
...
```

"**config/prod_2/application.properties**"
```sh
app.instance.number=2
```
"**config/prod_2/config.json**"
```json
{"config": "prod"}
```
"**config/prod_2/logback.xml**"
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <property name="LOG_PATH" value="@logPath@" />
...
```

Building the project using the command "**gradle build**" will generate the artifact with the following values on:
"**application.properties**":
```sh
app.instance.name=Jetty-Server
app.instance.number=1
```
"**config.json**"
```json
{"config": "test"}
```
"**logback.xml**"
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <property name="LOG_PATH" value="/logs/" />
...
```
Since no deployment environment was passed as parameter, the files remain unchanged allowing us to work with default values in a local development environment.

Now building the project using the command "**gradle build -Denv=prod_2 -DlogPath=/logs/test/**" will generate the artifact with the following values on:
"**application.properties**":
```sh
app.instance.name=Jetty-Server
app.instance.number=2
```
"**config.json**"
```json
{"config": "prod"}
```
"**logback.xml**"
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <property name="LOG_PATH" value="/logs/test/" />
...
```
As expected "**app.instance.number**" is now equal to "**2**", from the file inside "**prod_2**". The property "app.instance.name" wasn't modified, since it's not specified on "**config/prod_2/application.properties**". Also the file "**config.json**" was completelly replaced and on "**logback.xml**" the "**LOG_PATH**" property has the value indicated on the system property "**logPath**".

But that's not all, the values can be overriden using system properties. Using the command "**gradle build -Denv=prod_2 -Dapp.instance.number=5**" will generate the artifact with the following values on "**application.properties**":
```sh
app.instance.name=Jetty-Server
app.instance.number=5
```
This way the values are completely customizable and there is no need to include sensitive data inside the project's code, if any. However each deployment enviroment folder must have the same ".properties" file names and the keys must exist, even if they just have an empty value in order to be processed.

Although only values in "**app.instance.number**" were shown, it's important to note that all "**.properties**" files inside the deployment environment folder will be processed, like in this case for "**server.properties**".

### How to Apply the Plugin
Please follow the instructions given at: https://plugins.gradle.org/plugin/com.blogspot.nombre-temp.replace.properties.resources

### Demo
https://github.com/guillermo-varela/gradle-replace-properties-resources-plugin-demo

### Customization
Not only the values inside the values can be overriden, but also the name of the "**config**" folder containing the deployment environments can be changed adding an extra property to the Gradle project (in the build script before applying this plugin or in the file "gradle.properties") named "**configEnvironmentFolder**".

This way you could have a folder "environments" instead of "config" just with the following entry in "**gradle.properties**":
```sh
configEnvironmentFolder=environments
```

### How this plugin was developed
Check: [http://nombre-temp.blogspot.com/2015/12/desarrollando-un-plugin-basico-de-grade.html](http://nombre-temp.blogspot.com/2015/12/desarrollando-un-plugin-basico-de-grade.html)

License
----

MIT

   [PropertyFile]: <https://ant.apache.org/manual/Tasks/propertyfile.html>
   [processResources]: <https://docs.gradle.org/current/javadoc/org/gradle/language/jvm/tasks/ProcessResources.html>
   [JVM]: <https://en.wikipedia.org/wiki/Java_virtual_machine>
   [deployment environment]: <https://en.wikipedia.org/wiki/Deployment_environment>
