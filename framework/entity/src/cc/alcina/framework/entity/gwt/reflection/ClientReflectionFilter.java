package cc.alcina.framework.entity.gwt.reflection;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Stream;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;

public interface ClientReflectionFilter {
	boolean emitAnnotation(JClassType type,
			Class<? extends Annotation> annotationType);

	boolean emitProperty(JClassType type, String name);

	boolean emitType(JClassType type);

	boolean isWhitelistReflectable(JClassType t);

	void onGenerationComplete(AppImplRegistrations registrations,
			AppReflectableTypes reflectableTypes, Stream<JClassType> stream,
			String emitMessage) throws UnableToCompleteException;

	void updateReachableTypes(List<JClassType> types);

	boolean isVisibleType(JType type, AnnotationExistenceResolver resolver);
}
