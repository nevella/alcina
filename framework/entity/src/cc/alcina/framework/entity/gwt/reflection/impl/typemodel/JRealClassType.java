package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

public class JRealClassType
		implements com.google.gwt.core.ext.typeinfo.JRealClassType {
	public JRealClassType(TypeOracle typeOracle, Class clazz) {
	}

	@Override
	public JParameterizedType asParameterizationOf(JGenericType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Annotation> T
			findAnnotationInTypeHierarchy(Class<T> annotationType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JConstructor findConstructor(JType[] paramTypes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JField findField(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JMethod findMethod(String name, JType[] paramTypes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType findNestedType(String typeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Annotation[] getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JConstructor getConstructor(JType[] paramTypes)
			throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JConstructor[] getConstructors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType getEnclosingType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType getErasedType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JField getField(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JField[] getFields() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<? extends JClassType> getFlattenedSupertypeHierarchy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType[] getImplementedInterfaces() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JMethod[] getInheritableMethods() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getJNISignature() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLastModifiedTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public JType getLeafType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JMethod getMethod(String name, JType[] paramTypes)
			throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JMethod[] getMethods() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType getNestedType(String typeName) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType[] getNestedTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeOracle getOracle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JMethod[] getOverloads(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JMethod[] getOverridableMethods() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JPackage getPackage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getParameterizedQualifiedSourceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQualifiedBinaryName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQualifiedSourceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSimpleSourceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType[] getSubtypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType getSuperclass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAbstract() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JAnnotationType isAnnotation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean
			isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JArrayType isArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAssignableFrom(JClassType possibleSubtype) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAssignableTo(JClassType possibleSupertype) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JClassType isClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType isClassOrInterface() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDefaultInstantiable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEnhanced() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JEnumType isEnum() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isFinal() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JGenericType isGenericType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType isInterface() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMemberType() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPackageProtected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JParameterizedType isParameterized() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JPrimitiveType isPrimitive() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPrivate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProtected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPublic() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JRawType isRawType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isStatic() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JTypeParameter isTypeParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JWildcardType isWildcard() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEnhanced() {
		// TODO Auto-generated method stub
	}
}
