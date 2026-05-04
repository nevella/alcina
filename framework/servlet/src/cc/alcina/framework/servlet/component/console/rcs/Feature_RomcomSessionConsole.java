package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.servlet.component.console.Feature_AlcinaConsole;

/**
 * <h4>The romcom session console</h4>
 * <p>
 * List active + past sessions, filter by conditions such as 'long wait time',
 * 'large packet' etc
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_AlcinaConsole.class)
public interface Feature_RomcomSessionConsole extends Feature {
}
