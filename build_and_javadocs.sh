#!/bin/bash

# This Maven invocation will compile and install all Hotmoka and Takamaka
# jars in your local Maven repository, including Javadocs.
# Change the value for JAVA_HOME
# to whatever is correct in your machine. Remove -DskipTests if you
# what to run the tests as well. Modules get compiled and
# distributed inside the modules/ directory.

JAVA_HOME=/usr/lib/jvm/default-java mvn clean install -DskipTests
