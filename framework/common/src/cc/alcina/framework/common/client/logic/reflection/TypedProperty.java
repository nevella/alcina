package cc.alcina.framework.common.client.logic.reflection;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;

/**
 * <p>
 * Note that instances of this should be autogenerated, with the important
 * aspect really being the type of T (the property type)
 * 
 * <p>
 * Because that's defined in generated code, it's not reflectively verified,
 * (and that would be a little tricky since it's the type of the field in the
 * PackageProperties container)
 */
public class TypedProperty<S extends BaseSourcesPropertyChangeEvents, T>
		implements PropertyEnum {
	public Class<?> definingType;

	/*
	 * A marker interface for generated classes containing typed properties
	 */
	public interface Container {
	}

	String name;

	public TypedProperty(Class<S> definingType, String name) {
		this.definingType = definingType;
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	/**
	 * The point of the exercise - typed setters! Which fire
	 * propertyChangeEvents!
	 * 
	 * @param propertySource
	 * @param value
	 */
	public void set(S propertySource, T newValue) {
		propertySource.set(name, newValue);
	}
}