package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature.Type;

/**
 * <h4>Persistent settings support for the traversal UI</h4>
 * 
 * <pre>
 * <code>
 
## Persistence
 - settings are stored in browser localstorage
 - the rpc protocol has support for load/save
 - all settings are stored in 1 json doc at one localstorage key
 - the key is the remoteUI path

## Contents + UI
 - the settings tree itself is a listening tree (similar to a react store?) 
 - the settings ui will initially a list of path/values

## Testing
 - Check app startup works (loads the properties tree or initialises with a default tree)
 - Check settings UI is displayed
 - Check modification of one setting
 - Check persistence on reload


 * 
 * </code>
 * </pre>
 *
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_TraversalProcessView.class)
@Type.Ref(Type.Ui_support.class)
public interface Feature_TraversalProcessView_PersistentSettings
		extends Feature {
}
