package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.IsCancellable;
import cc.alcina.framework.common.client.state.MachineEvent;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineTransitionHandler;
import cc.alcina.framework.gwt.client.logic.state.AsyncCallbackTransitionHandler;

/*
 * 
 *
 */
public abstract class PersistenceCallbackTransitionHandler<T, M extends MachineModel>
		extends AsyncCallbackTransitionHandler<T, MachineModel> implements
		PersistenceCallback<T> {
	public PersistenceCallbackTransitionHandler(MachineEvent successEvent) {
		super(successEvent);
	}
}
