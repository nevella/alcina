package cc.alcina.framework.common.client.logic.domaintransform.undo;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

public class NullUndoManager implements TransformHistoryManager {
	public void prepareUndo(DomainTransformEvent event) {
	}

	public void undo(DomainTransformEvent event) {
	}
}
