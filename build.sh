#!/bin/bash

# This Maven invocation will compile and install all Hotmoka and Takamaka
# jars in your local Maven repository. Remove -DskipTests if you
# what to run the tests as well. Modules get compiled and
# distributed inside the modules/ directory.

mvn clean install -DskipTests
