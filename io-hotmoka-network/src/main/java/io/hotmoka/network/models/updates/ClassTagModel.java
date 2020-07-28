package io.hotmoka.network.models.updates;

import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.models.values.TransactionReferenceModel;

/**
 * The model of the class tag of an object.
 */
public class ClassTagModel {

	/**
	 * The name of the class of the object.
	 */
	public final String className;

	/**
	 * The transaction that installed the jar from where the class has been loaded.
	 */
	public final TransactionReferenceModel jar;

	/**
	 * Builds the model of the class tag of an object.
	 * 
	 * @param classTag the class tag
	 */
	public ClassTagModel(ClassTag classTag) {
		this.className = classTag.className;
		this.jar = new TransactionReferenceModel(classTag.jar);
	}

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