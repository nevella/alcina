package cc.alcina.framework.entity.gwt.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Action;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Rule;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Rules;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Type;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.TypeHierarchy;

/**
 * TODO:
 *
 * - implement resolution
 *
 * - implement filtering
 *
 * - For core support classes, check they're not already reachable via direct
 * code reachability. If they are, log their reachability differently (probably
 * new category - 'from code'
 *
 * - Get symbol analysis to work in obf,
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

		public RuleFilter(Rule rule) {
			this.rule = rule;
			this.directTypes = Arrays.stream(rule.classes()).map(Type::get)
					.filter(Objects::nonNull).collect(Collectors.toSet());
			this.subtypeHierarchyTypes = Arrays.stream(rule.subtypes())
					.map(Type::get).filter(Objects::nonNull)
					.map(reflectableTypes.byType::get).filter(Objects::nonNull)
					.flatMap(TypeHierarchy::typeAndSuperTypes)
					.collect(Collectors.toSet());
		}

		public Optional<Rule> match(Type type) {
			TypeHierarchy hierarchy = reflectableTypes.byType.get(type);
			if (rule.packageName().length() > 0) {
				if (hierarchy.packageName.startsWith(rule.packageName())) {
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
		populateRulesList();
	}

	private void populateRulesList() {
		Stack<Class> ruleBearers = new Stack<>();
		ruleBearers.push(getClass());
		while (ruleBearers.size() > 0) {
			Class<?> bearer = ruleBearers.pop();
			Rules rules = bearer.getAnnotation(Rules.class);
			Arrays.stream(rules.value()).map(RuleFilter::new)
					.forEach(this.rules::add);
			Arrays.stream(rules.ruleSets()).forEach(ruleBearers::push);
		}
	}

	@Override
	public boolean permit(Type type) {
		Optional<Rule> match = getMatch(type);
		if (match.isPresent()) {
			Ax.out("Explicit rule :: %s :: %s", match.get().action(), type);
			return match.get().action() == Action.INCLUDE;
		}
		// default to true
		return true;
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
}