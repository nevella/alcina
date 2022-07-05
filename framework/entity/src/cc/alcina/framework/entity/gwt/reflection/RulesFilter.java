package cc.alcina.framework.entity.gwt.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Action;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Condition;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Rule;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.RuleSet;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Rules;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Type;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.TypeHierarchy;

/**
 * 
 *
 * @author nreddel@barnet.com.au
 *
 */
public class RulesFilter extends ReachabilityLinkerPeer {
	List<RuleFilter> rules = new ArrayList<>();

	class RuleFilter {
		private Rule rule;

		private Set<Type> directTypes;

		private Set<Type> subtypeHierarchyTypes;

		private Condition condition;

		public RuleFilter(Rule rule) {
			this.rule = rule;
			condition = rule.condition();
			Class<? extends RuleSet> ruleSetHost = condition.ruleSet();
			if (ruleSetHost.getAnnotation(Condition.class) != null) {
				condition = ruleSetHost.getAnnotation(Condition.class);
			}
			this.directTypes = Arrays.stream(condition.classes()).map(Type::get)
					.filter(Objects::nonNull).collect(Collectors.toSet());
			this.subtypeHierarchyTypes = Arrays.stream(condition.subtypes())
					.map(Type::get).filter(Objects::nonNull)
					.map(reflectableTypes.byType::get).filter(Objects::nonNull)
					.flatMap(TypeHierarchy::subtypes)
					.collect(Collectors.toSet());
		}

		public Optional<Rule> match(Type type) {
			TypeHierarchy hierarchy = reflectableTypes.byType.get(type);
			if (condition.packageName().length() > 0) {
				if (hierarchy.packageName.startsWith(condition.packageName())) {
					return Optional.of(rule);
				}
			}
			if (directTypes.contains(type)) {
				return Optional.of(rule);
			}
			if (subtypeHierarchyTypes.contains(type)) {
				return Optional.of(rule);
			}
			return Optional.empty();
		}
	}

	public RulesFilter() {
	}

	private void populateRulesList() {
		Rules rules = getClass().getAnnotation(Rules.class);
		Arrays.stream(rules.value()).map(RuleFilter::new)
				.forEach(this.rules::add);
	}

	@Override
	public boolean permit(Type type) {
		Optional<Rule> match = getMatch(type);
		if (match.isPresent()) {
			return match.get().action() == Action.INCLUDE;
		}
		// default to true
		return true;
	}

	@Override
	protected boolean hasExplicitTypePermission(Type type) {
		return getMatch(type).isPresent();
	}

	private Optional<Rule> getMatch(Type type) {
		TypeHierarchy hierarchy = reflectableTypes.byType.get(type);
		for (RuleFilter filter : rules) {
			Optional<Rule> match = filter.match(type);
			if (match.isPresent()) {
				return match;
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<String> explain(Type type) {
		Optional<Rule> match = getMatch(type);
		return match.map(Rule::reason);
	}

	@Override
	protected void init(AppReflectableTypes reflectableTypes) {
		this.reflectableTypes = reflectableTypes;
		populateRulesList();
	}
}