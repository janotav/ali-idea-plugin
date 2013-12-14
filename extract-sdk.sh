#!/bin/sh

ant check-args >&2 &&

cat << HEADER | tee idea-sdk/pom.xml > install-libs.xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  This is a generated file, do not edit manually unless you know what you are doing.
  To regenerate the file issue following commands:

    $ cd ali-idea-plugin
    $ export IDEA_HOME=...
    $ ./extract-sdk.sh

-->
HEADER

cat << ANT_HEADER >> install-libs.xml
<project name="install-libs">
  <target name="install-libs">
ANT_HEADER

cat << 'POM_HEADER' >> idea-sdk/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ali-parent</artifactId>
        <groupId>com.hp.alm.ali</groupId>
        <version>3.7</version>
        <relativePath>../ali-parent</relativePath>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <name>${project.artifactId}:${project.version}</name>

    <artifactId>idea-sdk</artifactId>

    <dependencies>
POM_HEADER

for i in "$IDEA_HOME"/lib/*.jar ; do

    file=`basename $i .jar`

    {
        echo '    <exec executable="mvn">'
        echo '      <arg value="install:install-file"/>'
        echo '      <arg value="-DgroupId=com.intellij"/>'
        echo '      <arg value="-Dpackaging=jar"/>'
        echo '      <arg value="-Dfile=${IDEA_HOME}/lib/'${file}'.jar"/>'
        echo '      <arg value="-Dversion=${IDEA_VERSION}"/>'
        echo '      <arg value="-DartifactId='${file}'"/>'
        echo '    </exec>'

    } >> install-libs.xml

    cat <<DEPENDENCY >> idea-sdk/pom.xml
        <dependency>
            <groupId>com.intellij</groupId>
            <artifactId>$file</artifactId>
            <version>\${idea.version}</version>
        </dependency>
DEPENDENCY

done

cat << 'FOOTER' >> install-libs.xml
    <exec executable="mvn">
      <arg value="install:install-file"/>
      <arg value="-Dfile=${IDEA_HOME}/plugins/tasks/lib/tasks-api.jar"/>
      <arg value="-DgroupId=com.intellij.plugins"/>
      <arg value="-DartifactId=tasks-api"/>
      <arg value="-Dversion=${IDEA_VERSION}"/>
      <arg value="-Dpackaging=jar"/>
    </exec>
    <exec executable="mvn">
      <arg value="install:install-file"/>
      <arg value="-Dfile=${IDEA_HOME}/plugins/tasks/lib/tasks-core.jar"/>
      <arg value="-DgroupId=com.intellij.plugins"/>
      <arg value="-DartifactId=tasks-core"/>
      <arg value="-Dversion=${IDEA_VERSION}"/>
      <arg value="-Dpackaging=jar"/>
    </exec>
    <exec executable="mvn">
      <arg value="install:install-file"/>
      <arg value="-Dfile=${IDEA_HOME}/plugins/tasks/lib/tasks-java.jar"/>
      <arg value="-DgroupId=com.intellij.plugins"/>
      <arg value="-DartifactId=tasks-java"/>
      <arg value="-Dversion=${IDEA_VERSION}"/>
      <arg value="-Dpackaging=jar"/>
    </exec>
    <exec executable="mvn">
      <arg value="install:install-file"/>
      <arg value="-Dfile=${IDEA_HOME}/plugins/tasks/lib/jira-connector.jar"/>
      <arg value="-DgroupId=com.intellij.plugins"/>
      <arg value="-DartifactId=jira-connector"/>
      <arg value="-Dversion=${IDEA_VERSION}"/>
      <arg value="-Dpackaging=jar"/>
    </exec>
  </target>
</project>
FOOTER

cat << POM_FOOTER >> idea-sdk/pom.xml
    </dependencies>
</project>
POM_FOOTER
