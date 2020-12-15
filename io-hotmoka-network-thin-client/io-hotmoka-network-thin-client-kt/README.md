# io-hotmoka-network-thin-client-kt
Kotlin thin client for a hotmoka remote node.

### Setup
* install gradle on your local machine
* check jvm version of gradle  
run `gradle -version` and check if JVM is 11, if not then set JAVA_HOME 
  environment variable to point to your java 11 installation.  
  If the version didn't change then you should launch the build and tests
  commands with the `-Dorg.gradle.java.home=/JDK_PATH`
  example: `gradle build -x test -Dorg.gradle.java.home=/JDK_PATH`


### Build
`gradle build -x test`

### Tests
First, launch an empty hotmoka node locally which uses an EMPTY signature
* go to parent folder of the hotmoka project
* run `sh run_network_empty_memory_empty_signature`

Finally, run kotlin tests
* go to io-hotmoka-network-thin-client-kt folder
* run `gradle test`
