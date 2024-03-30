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

package io.hotmoka.node;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.internal.AbstractNodeImpl;

/**
 * A generic implementation of a node. The goal of this class is to provide
 * some shared machinery that can be useful in subclasses.
 */
@ThreadSafe
public abstract class AbstractNode extends AbstractNodeImpl {

	/**
	 * The version of Hotmoka used by the nodes.
	 */
	public final static String HOTMOKA_VERSION;

	/**
	 * Builds an abstract node.
	 */
	protected AbstractNode() {
	}

	/**
	 * Builds a shallow clone of the given node.
	 * 
	 * @param parent the node to clone
	 */
	protected AbstractNode(AbstractNode parent) {
		super(parent);
	}

	static {
		// we access the Maven properties from the pom.xml file of the project
		try (InputStream is = AbstractNode.class.getModule().getResourceAsStream("io.hotmoka.node.maven.properties")) {
			var mavenProperties = new Properties();
			mavenProperties.load(is);
			HOTMOKA_VERSION = mavenProperties.getProperty("hotmoka.version");
		}
		catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
}