Introduction
------------

This is the HP ALI for Intellij IDEA plugin. It provides integration with HP ALM 11, HP ALM 11.5x and HP Agile Manager
(beta). The implementation was originally conceived by HP and later open sourced under the Apache 2.0 license.

Download
--------

The binaries can be obtained through the [JetBrains Plugin Repository], either using the Plugin Manager directly in the
Intellij IDEA or download manually from the plugin [homepage].

[JetBrains Plugin Repository]: http://plugins.jetbrains.com/?idea
[homepage]: http://plugins.jetbrains.com/plugin?pr=idea&pluginId=6930

Building
--------

 * Both supported Intellij versions (12.1.1 and 13) need to be present for the build process to complete.
 * Requires Ant (for dependency extraction) and Maven (for actual build). If maven is not present on the system path,
   append "-Dmaven.executable=/path/to/maven/bin/mvn" when executing ant commands described bellow.

First you need to populate your local maven repository with Intellij IDEA libraries that are not available in the public
maven repository. Convenience Ant script is provided to simplify this task.

```
    $ ant install-sdk -DIDEA_HOME=<Idea Community 12.1.1 Home>
    $ ant install-sdk -DIDEA_HOME=<Idea Community 13 Home>
```

Once the dependencies are present in the local repository, you can perform actual build:

```
    $ ant build -DIDEA_HOME=<Idea Community 12.1.1 Home>
```

Resulting plugin is located in "ali-plugin-main/target/ali-idea-plugin.zip".

In case you are extending the plugin functionality make sure to also execute the tests in the context of the other
supported version to ensure there are no compatibility issue:

```
    $ ant test -DIDEA_HOME=<Idea Community 13 Home>
```