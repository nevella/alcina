package cc.alcina.framework.common.client.reflection.impl;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GwtScriptOnly;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.AnnotationResolver;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Method;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.ReflectiveAccess;
import cc.alcina.framework.common.client.reflection.ReflectiveAccess.Access;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;

/*
 * Overridden by super-source for GWT
 */
@GwtScriptOnly
public class ClassReflectorProvider {
	public static ClassReflector getClassReflector(Class clazz) {
		//sketch - have a <Class,createReflectorFunction> map - populate on module init - get it 
		throw new UnsupportedOperationException();
	}
	@GwtScriptOnly
	public static class ClassAnnotationResolver implements AnnotationResolver {
		private Class clazz;

		public ClassAnnotationResolver(Class clazz) {
			this.clazz = clazz;
		}

		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			throw new UnsupportedOperationException();
		}
	}
}
