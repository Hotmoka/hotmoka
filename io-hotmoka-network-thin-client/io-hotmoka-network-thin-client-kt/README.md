# io-hotmoka-network-thin-client-kt
Kotlin thin client for a hotmoka remote node.

### Build
`gradle build -x test`

### Tests
First, launch an empty hotmoka node locally which uses an EMPTY signature
* go to parent folder of the hotmoka project
* run `sh run_network_empty_memory_empty_signature`

Finally, run kotlin tests
* go to io-hotmoka-network-thin-client-kt folder
* run `gradle test`