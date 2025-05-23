/*
Copyright 2025 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.moka.internal;

import io.hotmoka.moka.internal.nodes.Config;
import io.hotmoka.moka.internal.nodes.Disk;
import io.hotmoka.moka.internal.nodes.Info;
import io.hotmoka.moka.internal.nodes.Manifest;
import io.hotmoka.moka.internal.nodes.Mokamint;
import io.hotmoka.moka.internal.nodes.Faucet;
import io.hotmoka.moka.internal.nodes.Takamaka;
import io.hotmoka.moka.internal.nodes.Tendermint;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(name = "nodes",
	header = "Manage Hotmoka nodes.",
	subcommands = {
		HelpCommand.class,
		Config.class,
		Disk.class,
		Info.class,
		Manifest.class,
		Mokamint.class,
		Faucet.class,
		Takamaka.class,
		Tendermint.class
	})
public class Nodes {
}