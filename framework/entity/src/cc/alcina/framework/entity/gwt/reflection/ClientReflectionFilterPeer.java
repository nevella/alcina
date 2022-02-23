package cc.alcina.framework.entity.gwt.reflection;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.util.Multiset;

public interface ClientReflectionFilterPeer {
	default <A extends Annotation> boolean emitAnnotation(JClassType type,
			Class<A> annotationType, String moduleName) {
		return true;
	}

	default boolean emitProperty(JClassType type, String propertyName,
			String moduleName) {
		return true;
	}

	default boolean emitType(JClassType type, String moduleName) {
		return true;
	}

	default Multiset<String, Set<JClassType>> getLegacyModuleTypeAssignments(
			Stream<JClassType> compilationTypes) {
		return new Multiset<>();
	}

	public static class Default implements ClientReflectionFilterPeer {
	}
}
