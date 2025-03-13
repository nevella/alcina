package cc.alcina.framework.common.client.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.PropertySerialization.TypesProvider_Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

/**
 * Specifies an instance that will be produced by an InstanceProvider - this can
 * be used by UIs to define their source (often a process) and ensure that the
 * process is executed
 */
@Bean(PropertySource.FIELDS)
@ReflectiveSerializer.Checks(ignore = true)
public final class InstanceQuery implements TreeSerializable {
	@Bean(PropertySource.FIELDS)
	@Registration.Self
	public abstract static class Parameter<V>
			implements TreeSerializable, Registration.AllSubtypes {
		public V value;

		@PropertySerialization(defaultProperty = true)
		public V getValue() {
			return value;
		}

		public void setValue(V value) {
			this.value = value;
		}

		public Parameter() {
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
