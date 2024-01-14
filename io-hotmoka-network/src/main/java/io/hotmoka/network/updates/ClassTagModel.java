/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.network.updates;

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.network.values.TransactionReferenceModel;

/**
 * The model of the class tag of an object.
 */
public class ClassTagModel {

	/**
	 * The name of the class of the object.
	 */
	public String className;

	/**
	 * The transaction that installed the jar from where the class has been loaded.
	 */
	public TransactionReferenceModel jar;

	/**
	 * Builds the model of the class tag of an object.
	 * 
	 * @param classTag the class tag
	 */
	public ClassTagModel(ClassTag classTag) {
		this.className = classTag.clazz.getName();
		this.jar = new TransactionReferenceModel(classTag.jar);
	}

	public ClassTagModel() {}

	/**
	 * Yields the class tag having this model, assuming that it belongs to the given object.
	 * 
	 * @param object the object whose class tag is referred
	 * @return the class tag
	 */
	public ClassTag toBean(StorageReference object) {
		return new ClassTag(object, className, jar.toBean());
	}
}