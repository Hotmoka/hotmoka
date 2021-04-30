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

package io.takamaka.code.lang;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A storage class is exported if its instances can be passed as parameters of methods
 * or constructors from outside the node. If a class is exported, it follows that everybody
 * can run any public method on its instances, even through the instance is apparently
 * encapsulated, since its reference is actually public in the store. This can be problematic
 * if the instances are modifiable throuogh their public API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ TYPE })
@Inherited
@Documented
public @interface Exported {
}