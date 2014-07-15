#!/bin/bash

IDEA_HOME=$1
version=$2

cat << HEADER | tee idea-sdk-$version/pom.xml > idea-sdk-$version/install-libs.xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  This is a generated file, do not edit manually unless you know what you are doing.
  To regenerate the file issue following command in the ali-idea-plugin directory (bash needed):

    $ ant extract-sdk IDEA_HOME=<Idea $version Directory>

-->
HEADER

cat << ANT_HEADER >> idea-sdk-$version/install-libs.xml
<project name="install-libs">
  <import file="../build.xml"/>
  <target name="install-libs" depends="setup-maven-linux,setup-maven-win">
ANT_HEADER

cat << POM_HEADER >> idea-sdk-$version/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ali-parent</artifactId>
        <groupId>com.hp.alm.ali</groupId>
        <version>3.10</version>
        <relativePath>../ali-parent</relativePath>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <name>\${project.artifactId}:\${project.version}</name>

    <artifactId>idea-sdk-$version</artifactId>

    <dependencies>
POM_HEADER

function addDependency {

    fullPath=$1
    groupId=$2
    prefix=$3

    file=`basename $fullPath .jar`

    {
        echo '    <exec executable="${maven.executable}">'
        echo '      <arg value="install:install-file"/>'
        echo '      <arg value="-DgroupId='$groupId'"/>'
        echo '      <arg value="-Dpackaging=jar"/>'
        echo '      <arg value="-Dfile=${IDEA_HOME}/'${prefix}'/'${file}'.jar"/>'
        echo '      <arg value="-Dversion='$version'"/>'
        echo '      <arg value="-DartifactId='${file}'"/>'
        echo '    </exec>'

    } >> idea-sdk-$version/install-libs.xml

    cat <<DEPENDENCY >> idea-sdk-$version/pom.xml
        <dependency>
            <groupId>$groupId</groupId>
            <artifactId>$file</artifactId>
            <version>$version</version>
        </dependency>
DEPENDENCY

}

for i in "$IDEA_HOME"/lib/*.jar ; do

    addDependency $i com.intellij lib

done

for i in "$IDEA_HOME"/plugins/tasks/lib/*.jar ; do

    addDependency $i com.intellij.plugins.tasks plugins/tasks/lib

done

for i in "$IDEA_HOME"/plugins/git4idea/lib/*.jar ; do

    addDependency $i com.intellij.plugins.git4idea plugins/git4idea/lib

done

for i in "$IDEA_HOME"/plugins/svn4idea/lib/*.jar ; do

    addDependency $i com.intellij.plugins.svn4idea plugins/svn4idea/lib

done

cat << FOOTER >> idea-sdk-$version/install-libs.xml
  </target>
</project>
FOOTER


cat << POM_FOOTER >> idea-sdk-$version/pom.xml
    </dependencies>
</project>
POM_FOOTER
