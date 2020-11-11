cwd=$(pwd)
explicitModules=$cwd"/modules/explicit"
automaticModules=$cwd"/modules/automatic"

java --module-path $explicitModules:$automaticModules --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartNetworkServiceWithEmptyMemoryNodeAndEmptySignature
