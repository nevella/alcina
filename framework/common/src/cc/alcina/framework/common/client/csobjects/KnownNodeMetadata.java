package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.KnownStatusRule.KnownStatusRuleImpl;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Reflected;

@Bean
public class KnownNodeMetadata implements Serializable {
	private Type type;

	private List<KnownNodeProperty> knownNodeProperties = new ArrayList<>();

	private KnownStatusRuleImpl statusRule;

	public List<KnownNodeProperty> getKnownNodeProperties() {
		return knownNodeProperties;
	}

	public KnownNodeProperty getPropertyMetadata(String name) {
		return getKnownNodeProperties().stream()
				.filter(n -> n.getName().equals(name)).findFirst().orElse(null);
	}

	public KnownStatusRuleImpl getStatusRule() {
		return statusRule;
	}

	public Type getType() {
		return type;
	}

	public void setKnownNodeProperties(
			List<KnownNodeProperty> knownNodeProperties) {
		this.knownNodeProperties = knownNodeProperties;
	}

	public void setStatusRule(KnownStatusRuleImpl statusRule) {
		this.statusRule = statusRule;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Bean
	public static class KnownNodeProperty implements Serializable {
		private KnownStatusRuleImpl statusRule;

		private String name;

		private String typeName;

		public String getName() {
			return name;
		}

		public KnownStatusRuleImpl getStatusRule() {
			return statusRule;
		}

		public String getTypeName() {
			return typeName;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setStatusRule(KnownStatusRuleImpl statusRule) {
			this.statusRule = statusRule;
		}

		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}
	}

	@Reflected
	public enum Type {
		Job, Other
	}
}
