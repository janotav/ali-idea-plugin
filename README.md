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

If you wish to build the plugin from the sources you first need to populate your local maven repository with
Intellij IDEA libraries that are not available in the public maven repository. Convenience ANT script is provided to
simplify this task. Before using this script make sure that IDEA_HOME points to your IDEA installation.

```
    $ ant build
```

When invoked this takes care of the following:

1. installs relevant jars from the IDEA_HOME installation using "mvn install:install-file"
2. invokes "mvn package" overriding version identifiers to match your IDEA version
3. resulting plugin is located in "target/ali-idea-plugin.zip"

Alternatively if you need to tweak the build somehow, you may also run:

```
    $ ant install-libs
```

This installs relevant jars into your local maven repository. Then you need to specify correct "idea.build" and
"idea.version" properties inside the "ali-plugin-main/pom.xml" and the maven project should be buildable.