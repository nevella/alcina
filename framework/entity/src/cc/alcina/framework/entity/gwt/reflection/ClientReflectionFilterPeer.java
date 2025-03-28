package cc.alcina.framework.entity.gwt.reflection;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.ReflectionModule;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.ModuleTypes;

public interface ClientReflectionFilterPeer {
	default <A extends Annotation> boolean emitAnnotation(JClassType type,
			Class<A> annotationType, String moduleName) {
		return true;
	}

	default boolean emitProperty(JClassType type, String propertyName,
			String moduleName) {
		return true;
	}

	default Boolean emitType(JClassType type, String moduleName) {
		return null;
	}

	default Multiset<String, Set<JClassType>> getLegacyModuleTypeAssignments(
			Stream<JClassType> compilationTypes) {
		File legacyTypesFile = ReachabilityData
				.getReachabilityFile("legacy-reachability.json");
		if (legacyTypesFile.exists()) {
			ModuleTypes moduleTypes = ReachabilityData
					.deserialize(ModuleTypes.class, legacyTypesFile);
			moduleTypes.generateLookup();
			Multiset<String, Set<JClassType>> result = new Multiset<>();
			compilationTypes.forEach(t -> {
				String moduleName = moduleTypes
						.moduleFor(t.getQualifiedSourceName());
				if (ReflectionModule.Modules.provideIsFragment(moduleName)) {
					result.add(moduleName, t);
				}
			});
			return result;
		} else {
			return new Multiset<>();
		}
	}

	default boolean isVisibleType(JType type,
			AnnotationExistenceResolver existenceResolver) {
		return existenceResolver.has((JClassType) type, Bean.class);
	}

	default boolean isWhitelistReflectable(JClassType t) {
		return false;
	}

	public static class Default implements ClientReflectionFilterPeer {
	}
}
