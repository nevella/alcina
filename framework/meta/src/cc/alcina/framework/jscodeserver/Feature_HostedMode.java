package cc.alcina.framework.jscodeserver;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 *
 */
/*
 * @formatter:off
 * (breakout)

- Speed checks - use async profiler
- Check previous classloader fully onloaded (profile)
- get data about long pauses receiving ws data - gc?

 * @formatter:on
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_HostedMode extends Feature {
}
