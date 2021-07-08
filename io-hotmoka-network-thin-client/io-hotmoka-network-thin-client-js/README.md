# hotmoka JS
JavaScript/TypeScript thin client for a remote hotmoka node.

## Install packages
`npm i`

## Build hotmoka
`npm run bundle`

## Build hotmoka with types
`npm run bundle:all`

## Testing
First, launch an initialized hotmoka node locally which uses an EMPTY signature:
* go to parent folder of the hotmoka project
* run `sh run_network_initialized_memory_empty_signature`
* run `npm run test`