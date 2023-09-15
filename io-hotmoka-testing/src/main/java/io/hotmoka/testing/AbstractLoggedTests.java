/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.testing;

import io.hotmoka.testing.internal.LoggedTestsImpl;

/**
 * Shared code of test classes. It configures the logging system in such a way
 * to log each test class in its own log file and to report a header before the logs of each test.
 * Test classes just need to subclass this class in order to use these features.
 */
public abstract class AbstractLoggedTests extends LoggedTestsImpl {
}