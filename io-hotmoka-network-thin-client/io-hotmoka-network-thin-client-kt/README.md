# io-hotmoka-network-thin-client-kt
Kotlin thin client for a hotmoka remote node.

### Setup
1. install gradle on your local machine
2. set JAVA_HOME environment variable to point to your JDK 11 path intallation
3. check jvm version of gradle  
run `./gradlew -version` and check if JVM is 11, if not then follow step 2.  
  If the version didn't change then you should launch the build and tests
  commands with the `-Dorg.gradle.java.home=/JDK_PATH`
  example: `gradle build -x test -Dorg.gradle.java.home=/JDK_PATH`


### Build
`./gradlew build -x test`

### Tests 
##### Uninitialized Hotmoka node  
First, launch an empty hotmoka node locally which uses an EMPTY signature:
* go to parent folder of the hotmoka project
* run `sh run_network_empty_memory_empty_signature`

Finally, run kotlin tests
* go to io-hotmoka-network-thin-client-kt folder
* run `./gradlew test --tests UninitializedRemoteNodeTest`   

##### Initialized Hotmoka node  
First, launch an initialized hotmoka node locally which uses an EMPTY signature:
* go to parent folder of the hotmoka project
* run `sh run_network_initialized_memory_empty_signature`

Finally, run kotlin tests
* go to io-hotmoka-network-thin-client-kt folder
* run `./gradlew test --tests InitializedRemoteNodeTest`
