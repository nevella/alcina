package cc.alcina.framework.entity.gwt.reflection;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;

class ReachedClassFilter implements ClientReflectionFilter {
	private String className;

	private String lastModuleName;

	public ReachedClassFilter(String lastModuleName, String className) {
		this.lastModuleName = lastModuleName;
		this.className = className;
	}

	@Override
	public boolean emitAnnotation(JClassType type,
			Class<? extends Annotation> annotationType) {
		return emit(type);
	}

	@Override
	public boolean emitProperty(JClassType type, String name) {
		return emit(type);
	}

	@Override
	public boolean emitType(JClassType type) {
		return emit(type);
	}

	@Override
	public void onGenerationComplete(AppImplRegistrations registrations,
			AppReflectableTypes reflectableTypes, Stream<JClassType> stream,
			String emitMessage) throws UnableToCompleteException {
		Ax.out("DevMode - module %s - reflected class  %s", lastModuleName,
				className);
	}

	private boolean emit(JClassType type) {
		return type.getQualifiedBinaryName().equals(className);
	}
}
