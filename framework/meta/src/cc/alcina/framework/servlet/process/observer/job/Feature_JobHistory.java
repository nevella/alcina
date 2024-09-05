package cc.alcina.framework.servlet.process.observer.job;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * This component provides a system to record + display job events for deep
 * debugging
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_JobHistory extends Feature {
}
