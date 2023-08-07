package com.google.gwt.dev.javac.typemodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;

public class JavacTypeBounds {
	public final List<JClassType> bounds;

	/**
	 * See also (parallels, but with jdk type model) :
	 * cc.alcina.framework.entity.gwt.reflection.impl.typemodel.JClassType.Members.computeBounds()
	 *
	 *
	 * @param objectType
	 *
	 *
	 *
	 */
	public JavacTypeBounds(JClassType type, JClassType objectType) {
		bounds = new ArrayList<>();
		// find the nearest ParameterizedType ancestor of type (including
		// self)
		JClassType cursor = type;
		if (cursor instanceof JTypeParameter) {
			JTypeParameter typeParameter = (JTypeParameter) cursor;
			Arrays.stream(typeParameter.getBounds()).forEach(bounds::add);
		} else {
			while (cursor != null) {
				if (cursor instanceof JParameterizedType) {
					addToBounds(cursor, objectType);
					break;
				}
				JClassType superclass = cursor.getSuperclass();
				if (superclass instanceof JParameterizedType) {
					addToBounds(superclass, objectType);
					break;
				}
				boolean matchedInterface = false;
				for (JClassType superInterface : cursor
						.getImplementedInterfaces()) {
					if (superInterface instanceof JParameterizedType) {
						addToBounds(superInterface, objectType);
						matchedInterface = true;
						break;
					}
				}
				if (matchedInterface) {
					break;
				}
				if (cursor instanceof JRealClassType) {
					cursor = cursor.getSuperclass();
				} else if (cursor instanceof JGenericType) {
					cursor = cursor.getSuperclass();
				} else if (cursor instanceof JRawType) {
					cursor = cursor.getSuperclass();
				} else if (cursor instanceof JArrayType) {
					// not a simple bound
					break;
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	void addToBounds(JClassType clazz, JClassType objectType) {
		JParameterizedType nearestGenericSupertype = (JParameterizedType) clazz;
		Arrays.stream(nearestGenericSupertype.getTypeArgs())
				.map(arg -> (arg instanceof JWildcardType) ? objectType : arg)
				.forEach(bounds::add);
	}

	public static class Computed {
		private Map<JClassType, JavacTypeBounds> computed = new LinkedHashMap<>();

		public JavacTypeBounds get(JClassType type, JClassType objectType) {
			return computed.computeIfAbsent(type,
					t -> new JavacTypeBounds(t, objectType));
		}
	}
}
