package cc.alcina.framework.common.client.reflection.impl;

import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ClientReflections;

/*
 * gwt client implementation
 */
public class ClassReflectorProvider {
	public static ClassReflector getClassReflector(Class clazz) {
		return ClientReflections.getClassReflector(clazz);
	}
}
