Introduction
------------

This is the HP ALI for Intellij IDEA plugin. It provides integration with HP ALM 11, HP ALM 11.5x, HP ALM 12 and
HP Agile Manager (beta). The implementation was originally conceived by HP and later open sourced under the Apache 2.0
license.

The plugin is primarily developed for the Java IDE, it however may work with other flavours. If you encounter any issues
with RubyMine, PyCharm or others, feel free to open an issue and it will be investigated.

Download
--------

The binaries can be obtained through the [JetBrains Plugin Repository], either using the Plugin Manager directly in the
Intellij IDEA or download manually from the plugin [homepage].

[JetBrains Plugin Repository]: http://plugins.jetbrains.com/?idea
[homepage]: http://plugins.jetbrains.com/plugin?pr=idea&pluginId=6930

Building
--------

 * Intellij community version 14.1 is supported. Building against other minor version or ultimate edition
   is technically possible, but might require manual changes to the dependencies.
 * Requires Ant (for dependency extraction and build invocation) and Maven (for actual build). If maven is not present
   on the system path, append "-Dmaven.executable=/path/to/maven/bin/mvn" when executing ant commands described bellow.

First you need to populate your local maven repository with Intellij IDEA libraries that are not available in the public
maven repository. Convenience Ant script is provided to simplify this task.

```
    $ ant install-sdk -DIDEA_HOME=<Idea Community 14.1 Home>
```

Once the dependencies are present in the local repository, you can perform actual build:

```
    $ ant build -DIDEA_HOME=<Idea Community 14.1 Home>
```

Resulting plugin is located in "ali-plugin-main/target/ali-idea-plugin.zip".
