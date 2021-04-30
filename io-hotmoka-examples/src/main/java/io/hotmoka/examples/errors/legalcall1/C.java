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

package io.hotmoka.examples.errors.legalcall1;

public class C extends Super {

	@Override
	public int hashCode() {
		// this is illegal, although the receiver redefines hashCode():
		// the reason is that the @MustRedefineHashCode annotation on a method
		// does not allow that method to be called through invokespecial, with
		// a target method whose static resolution is Object.hashCode();
		// in other terms, the check for the redefinition of hashCode() is done
		// statically, at verification time, from the declared type of the receiver
		return super.hashCode();
	}
}