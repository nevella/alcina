package cc.alcina.framework.common.client.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.PropertySerialization.TypesProvider_Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaCollections;

/**
 * Specifies an instance that will be produced by an InstanceProvider - this can
 * be used by UIs to define their source (often a process) and ensure that the
 * process is executed
 */
@Bean(PropertySource.FIELDS)
@ReflectiveSerializer.Checks(ignore = true)
public final class InstanceQuery implements TreeSerializable {
	@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
	public abstract static class TransientParameter<V> extends Parameter<V> {
	}

	/**
	 * Note for serialization (from the FlatTreeSerializer doc):
	 * 
	 * <p>
	 * * Objects - particularly generic objects such as InstanceQuery.Parameter
	 * with a complex type parameter - must have en empty value set in the
	 * constructor for deserialization to work
	 * 
	 */
	@Bean(PropertySource.FIELDS)
	@Registration.Self
	@ReflectiveSerializer.Checks(ignore = true)
	public abstract static class Parameter<V>
			implements TreeSerializable, Registration.AllSubtypes {
		public V value;

		@PropertySerialization(defaultProperty = true)
		public V getValue() {
			return value;
		}

		public static class Support {
			static volatile Map<Class<?>, Class<? extends Parameter>> valueTypeParameterType;

			public static synchronized Parameter
					getSoleParameterOfType(Class clazz) {
				if (valueTypeParameterType == null) {
					valueTypeParameterType = AlcinaCollections.newUnqiueMap();
					List<Class<? extends Parameter>> candidates = Registry
							.query(Parameter.class).registrations()
							.filter(t -> Reflections.at(t)
									.firstGenericBound() == clazz)
							.toList();
					if (candidates.size() == 1) {
						valueTypeParameterType.put(clazz, candidates.get(0));
					}
				}
				return Reflections
						.newInstance(valueTypeParameterType.get(clazz));
			}
		}

		public void setValue(V value) {
			this.value = value;
		}

		public Parameter() {
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() ^ Objects.hashCode(value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj.getClass() == getClass()) {
				return Objects.equals(((Parameter) obj).getValue(), getValue());
			} else {
				return false;
			}
		}

		public Parameter(V value) {
			this.value = value;
		}

		public Parameter<V> withValue(V value) {
			this.value = value;
			return this;
		}

		public String name() {
			return getClass().getSimpleName().toLowerCase();
		}
	}

	/**
	 * The result type. Unbounded, so problematic for reflective data pruning
	 * (GWT)..probably a FIXME
	 */
	@PropertySerialization(path = "querytype")
	public Class type;

	@PropertySerialization(
		defaultProperty = true,
		typesProvider = { TypesProvider_Registry.class,
				InstanceQuery.Parameter.class })
	public List<Parameter<?>> parameters = new ArrayList<>();

	@Property.Not
	public boolean isBlank() {
		return type == null;
	}

	public <T> InstanceOracle.Query<T> toOracleQuery() {
		return new InstanceOracle.Query<>(type).addParameters(parameters);
	}

	public InstanceQuery withType(Class type) {
		this.type = type;
		return this;
	}

	public InstanceQuery addParameters(Parameter<?>... parameters) {
		Arrays.stream(parameters).forEach(this.parameters::add);
		return this;
	}
}
