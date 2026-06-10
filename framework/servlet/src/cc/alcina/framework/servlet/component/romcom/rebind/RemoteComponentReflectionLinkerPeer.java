package cc.alcina.framework.servlet.component.romcom.rebind;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.Rule;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reachability.RuleSet;
import cc.alcina.framework.entity.gwt.reflection.RulesFilter;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.util.KeyboardShortcuts;

@Reachability.Rules({
		@Rule(
			action = Reachability.Action.EXCLUDE,
			reason = "Not reached in code",
			condition = @Reachability.Condition(
				packageName = "cc.alcina.framework.gwt.client.dirndl")),
		@Rule(
			action = Reachability.Action.EXCLUDE,
			reason = "Not reached in code",
			condition = @Reachability.Condition(
				packageName = "cc.alcina.framework.gwt.client.entity.place")),
		@Rule(
			action = Reachability.Action.EXCLUDE,
			reason = "Not reached in code",
			condition = @Reachability.Condition(
				packageName = "cc.alcina.framework.gwt.client.module")) })
public class RemoteComponentReflectionLinkerPeer extends RulesFilter {
	@Reachability.Condition()
	public static class Exclude_Dirndl implements RuleSet {
	}
}