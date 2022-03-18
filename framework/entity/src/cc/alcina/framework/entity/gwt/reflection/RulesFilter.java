package cc.alcina.framework.entity.gwt.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

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
	List<Rule> rules = new ArrayList<>();

	public RulesFilter() {
		populateRulesList();
	}

	private void populateRulesList() {
		Stack<Class> ruleBearers = new Stack<>();
		ruleBearers.push(getClass());
		while (ruleBearers.size() > 0) {
			Class<?> bearer = ruleBearers.pop();
			Rules rules = bearer.getAnnotation(Rules.class);
			Arrays.stream(rules.value()).forEach(this.rules::add);
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
		for (Rule rule : rules) {
			if (rule.packageName().length() > 0) {
				if (hierarchy.packageName.startsWith(rule.packageName())) {
					return Optional.of(rule);
				}
			}
			if (rule.classes().length > 0) {
				for (int idx = 0; idx < rule.classes().length; idx++) {
					if (type.matchesClass(rule.classes()[idx])) {
						return Optional.of(rule);
					}
				}
			}
			if (rule.subtypes().length > 0) {
				for (int idx = 0; idx < rule.subtypes().length; idx++) {
					Class test = rule.subtypes()[idx];
					for (Type cursor : hierarchy.typeAndSuperTypes) {
						if (cursor.matchesClass(test)) {
							return Optional.of(rule);
						}
					}
				}
			}
		}
		return Optional.empty();
	}
}