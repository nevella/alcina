package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.KnownStatusRule.KnownStatusRuleImpl;

public class KnownNodeMetadata implements Serializable{
	public enum Type {
		Job, Other
	}

	public Type type;

	public List<KnownNodeProperty> knownNodeProperties = new ArrayList<>();

	public KnownStatusRuleImpl statusRule;

	public static class KnownNodeProperty implements Serializable{
		public KnownStatusRuleImpl statusRule;

		public String name;

		public String typeName;
	}

	public KnownNodeProperty getPropertyMetadata(String name) {
		return knownNodeProperties.stream().filter(n -> n.name.equals(name))
				.findFirst().orElse(null);
	}
}
