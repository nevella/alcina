package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * <p>
 * Models a bean property, client/server
 * 
 * <p>
 * FIXME - dirndl - the current 'java transient modifier -&gt; non-property'
 * should be removed - but that'll need a refactor task to check existing usages
 * (replace with Proeprty.Not or AlcinaTransient as appropriate) - it's
 * perfectly valid to have a transient property, and it's better to use private
 * rather than transient to denote non-property via a java modifier
 * 
 * <p>
 * Note re reflection - annotation access does *not* resolve - it essentially
 * tracks JDK annotation behaviour. Use {@link AnnotationLocation} to resolve
 * annotations at a property. e.g. *don't* expect Property.has(Bean.class) to
 * work, since that's a property with a resolution strategy
 */
public class Property implements HasAnnotations {
	private final String name;

	private final Method getter;

	private final Method setter;

	private final Class type;

	private final Class owningType;

	private final AnnotationProvider annotationProvider;

	private final Class declaringType;

	private final TypeBounds typeBounds;

	public interface Has {
		Property provideProperty();
	}

	public Property(Property property) {
		this(property.name, property.getter, property.setter, property.type,
				property.owningType, property.declaringType,
				property.typeBounds, property.annotationProvider);
	}

	public Property(String name, Method getter, Method setter,
			Class propertyType, Class owningType, Class declaringType,
			TypeBounds typeBounds, AnnotationProvider annotationProvider) {
		this.name = name;
		this.getter = getter;
		this.setter = setter;
		this.type = propertyType;
		this.owningType = owningType;
		this.declaringType = declaringType;
		this.typeBounds = typeBounds;
		this.annotationProvider = annotationProvider;
	}

	@Override
	public <A extends Annotation> A annotation(Class<A> annotationClass) {
		return annotationProvider.getAnnotation(annotationClass);
	}

	@Override
	public <A extends Annotation> List<A>
			annotations(Class<A> annotationClass) {
		return annotationProvider.getAnnotations(annotationClass);
	}

	public void copy(Object from, Object to) {
		set(to, get(from));
	}

	public Object get(Object bean) {
		return resolveGetter(bean).invoke(bean, CommonUtils.EMPTY_OBJECT_ARRAY);
	}

	/**
	 * The declarer (location of explicit get/set/is method) (ClassReflector
	 * type) of this property
	 */
	public Class getDeclaringType() {
		return this.declaringType;
	}

	public Method getMethod(boolean get) {
		return get ? getter : setter;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * The owner (ClassReflector type) of this property
	 */
	public Class getOwningType() {
		return owningType;
	}

	public Class getType() {
		return type;
	}

	public Class getType(Object bean) {
		return isWriteOnly() ? getType() : resolveGetter(bean).getReturnType();
	}

	public TypeBounds getTypeBounds() {
		return this.typeBounds;
	}

	public <A extends Annotation> boolean has(Class<A> annotationClass) {
		return annotationProvider.hasAnnotation(annotationClass);
	}

	public boolean isDefiningType(Class type) {
		return type == owningType;
	}

	public boolean isPropertyName(String name) {
		return Objects.equals(name, this.name);
	}

	public boolean isReadable() {
		return getter != null;
	}

	public boolean isReadOnly() {
		return setter == null;
	}

	public boolean isReadWrite() {
		return getter != null && setter != null;
	}

	public boolean isWriteable() {
		return setter != null;
	}

	public boolean isWriteOnly() {
		return getter == null;
	}

	public boolean provideNotDefaultIgnoreable() {
		switch (name) {
		case "class":
		case "propertyChangeListeners":
			return false;
		default:
			return true;
		}
	}

	public boolean provideReadWriteNonTransient() {
		return isReadWrite() && !name.equals("propertyChangeListeners");
	}

	public boolean provideRenderable() {
		return isReadable() && provideNotDefaultIgnoreable();
	}

	protected Method resolveGetter(Object bean) {
		return bean.getClass() == owningType ? getter
				: Reflections.at(bean).property(name).getter;
	}

	protected Method resolveSetter(Object bean) {
		return bean.getClass() == owningType ? setter
				: Reflections.at(bean).property(name).setter;
	}

	public void set(Object bean, Object newValue) {
		resolveSetter(bean).invoke(bean, new Object[] { newValue });
	}

	public String toLocationString() {
		return Ax.format("%s.%s", owningType.getSimpleName(), name);
	}

	@Override
	public String toString() {
		return Ax.format("%s.%s : %s", owningType.getSimpleName(), name,
				type.getSimpleName());
	}

	public static class NameComparator implements Comparator<Property> {
		@Override
		public int compare(Property o1, Property o2) {
			return o1.name.compareTo(o2.name);
		}
	}

	/**
	 * *NOT* a property method or field. Will be inherited if applied to an
	 * interface
	 * 
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Resolution(
		inheritance = { Inheritance.PROPERTY },
		mergeStrategy = Not.MergeStrategy.class)
	@ClientVisible
	public @interface Not {
		@Reflected
		public static class MergeStrategy extends
				AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOnly<Not> {
		}
	}

	@Override
	public boolean isProperty(Class<?> owningType, String propertyName) {
		if (owningType == null) {
			return Objects.equals("*", propertyName);
		} else {
			return owningType == this.owningType
					&& Objects.equals(name, propertyName);
		}
	}

	@Override
	public boolean isClass(Class locationClass, String propertyName) {
		return false;
	}

	@Override
	public boolean isOrIsPropertyAncestor(Property property) {
		if (property == this) {
			return true;
		}
		return declaringType == property.declaringType
				&& Objects.equals(getName(), property.getName());
	}

	public Class firstGenericBound() {
		return getTypeBounds().bounds.get(0);
	}
}
