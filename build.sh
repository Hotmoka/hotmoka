#!/bin/bash

# This Maven invocation will compile and install all Hotmoka and Takamaka
# jars in your local Maven repository. Change the value for JAVA_HOME
# to whatever is correct in your machine. Remove -DskipTests if you
# what to run the tests as well.
#
# Its execution generates the following useful files:
# hotmoka-and-takamaka-assembly/target/hotmoka-and-takamaka-assembly-1.0.0-jar-with-dependencies.jar
# target/hotmoka-and-takamaka-assembly-1.0.0-javadoc.jar
#
# containing all classes and JavaDocs, respectively, of the Hotmoka and Takamaka projects.
# Moreover, inside each child project, there will be a "target" directory containing the jar of the
# classes of that project only.

JAVA_HOME=/usr/lib/jvm/default-java mvn clean install -DskipTests javadoc:aggregate-jar
