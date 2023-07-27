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
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ClientReflections;
import cc.alcina.framework.common.client.reflection.Method;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.ReflectiveAccess;
import cc.alcina.framework.common.client.reflection.ReflectiveAccess.Access;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
/*
 * gwt client implementation
 */
public class ClassReflectorProvider {
	public static ClassReflector getClassReflector(Class clazz) {
		return ClientReflections.getClassReflector(clazz);
	}
	/*
	 * copy of non-emul nested type
	 */
	@GwtScriptOnly
	public static class ClassAnnotationProvider implements AnnotationProvider {
		private Class clazz;

		public ClassAnnotationProvider(Class clazz) {
			this.clazz = clazz;
		}

		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			throw new UnsupportedOperationException();
		}
		@Override
		public <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass) {
			throw new UnsupportedOperationException();
		}
	}
}
