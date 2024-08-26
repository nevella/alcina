package cc.alcina.framework.servlet.process.observer.mvcc;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * This component provides a system to record + display mvcc events for deep
 * debugging
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_MvccHistory extends Feature {
}
