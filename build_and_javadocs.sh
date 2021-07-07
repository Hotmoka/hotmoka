#!/bin/bash

# This Maven invocation will compile all Hotmoka and Takamaka
# jars and generate their JavaDocs. Change the value for JAVA_HOME
# to whatever is correct in your machine.

JAVA_HOME=/usr/lib/jvm/default-java mvn clean install -Drestricted
