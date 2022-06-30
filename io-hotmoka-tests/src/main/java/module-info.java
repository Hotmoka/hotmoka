/*
Copyright 2021 Fausto Spoto

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

open module io.takamaka.code.tests {
	requires io.hotmoka.beans;
	requires io.hotmoka.nodes;
	requires io.hotmoka.helpers;
	requires io.hotmoka.local;
	requires io.hotmoka.crypto;
	requires io.hotmoka.memory;
	requires io.hotmoka.constants;
	requires io.hotmoka.instrumentation;
	requires io.hotmoka.verification;
	requires org.junit.jupiter.api;
	requires maven.model;
	requires plexus.utils;
	requires java.logging;
}