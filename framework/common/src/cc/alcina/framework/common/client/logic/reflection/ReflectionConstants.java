package cc.alcina.framework.common.client.logic.reflection;

import com.google.gwt.core.client.GWT;
//not reached in hosted mode, use the other copy
public class ReflectionConstants {
	public static final boolean USE_NON_JVM_IN_HOSTED = false;

	public static boolean useJvmIntrospector() {
		return !useGeneratedIntrospector();
	}
	public static boolean useGeneratedIntrospector() {
		return GWT.isScript() || USE_NON_JVM_IN_HOSTED;
	}

}
