package cc.alcina.framework.servlet.component.test.rebind;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Action;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Condition;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Rule;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.RuleSet;
import cc.alcina.framework.entity.gwt.reflection.RulesFilter;
import cc.alcina.framework.entity.gwt.reflection.RulesFilter.IncludedAlcinaMergeStrategies;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.servlet.component.test.rebind.AlcinaGwtTestReflectionLinkerPeer.Included_Custom;

@Rule(
	action = Action.INCLUDE,
	reason = "Explicitly add, pending fixes to reachability algorithm",
	condition = @Condition(ruleSet = IncludedAlcinaMergeStrategies.class))
@Rule(
	action = Action.INCLUDE,
	reason = "Not algorithmically reachable (event)",
	condition = @Condition(ruleSet = Included_Custom.class))
public class AlcinaGwtTestReflectionLinkerPeer extends RulesFilter {
	@Condition(
		subtypes = { ContextResolver.Default.class,
				DirectedContextResolver.class })
	public static class Included_Custom implements RuleSet {
	}
}