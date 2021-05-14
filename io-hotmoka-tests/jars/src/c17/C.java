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

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {
    private final int x;

    public C() {
	x = 17;
    }

    public int get() {
	return x;
    }
}
