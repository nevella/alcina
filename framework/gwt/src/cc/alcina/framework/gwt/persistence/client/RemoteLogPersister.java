package cc.alcina.framework.gwt.persistence.client;

import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.state.EnumMachine;
import cc.alcina.framework.common.client.state.EnumMachine.EnumMachineState;
import cc.alcina.framework.common.client.state.MachineEvent;
import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;
import cc.alcina.framework.common.client.state.MachineListener;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineState;
import cc.alcina.framework.common.client.state.MachineTransitionHandler;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.state.AsyncCallbackTransitionHandler;
import cc.alcina.framework.gwt.client.util.ClientUtils;

//state change listener (OFFLINE), try on state change, loose on app shutdown
public class RemoteLogPersister {
	private enum State {
		GET_LOG_RECORD_RANGE, CHECK_ONLINE, ADD_RECORDS, PUSH, DELETE
	}

	public static int PREFERRED_MAX_PUSH_SIZE = 30000;// bytes

	class RemoteLogPersisterMachine extends EnumMachine<State, MachineModel> {
		IntPair logRecordRange = null;

		int idCtr = 0;

		int firstAddedThisBuffer = 0;

		int lastAddedThisBuffer = 0;

		private MachineEventImpl keepAdding;

		private StringBuilder buffer = new StringBuilder();

		public boolean maybeOffline;

		public RemoteLogPersisterMachine() {
			super();
			init(State.class);
			buffer = new StringBuilder();
			EnumMachineState<State> addingState = getStateFor(State.ADD_RECORDS);
			EnumMachineState<State> checkOnlineState = getStateFor(State.CHECK_ONLINE);
			keepAdding = new MachineEventImpl("keep-adding", addingState,
					addingState);
		}

		@Override
		protected MachineTransitionHandler getTransitionHandlerFor(
				EnumMachineState<State> state, MachineEventImpl event) {
			switch (state.getEnumValue()) {
			case GET_LOG_RECORD_RANGE:
				return new LogRecordRangeGetter(event);
			case CHECK_ONLINE:
				return new LogRecordCheckOnline(event);
			case ADD_RECORDS:
				return new LogRecordGetter(event);
			case PUSH:
				return new LogRecordRemotePusher(event);
			case DELETE:
				return new LogRecordRangeDeleter(event);
			}
			return null;
		}

		class LogRecordRangeGetter extends
				PersistenceCallbackTransitionHandler<IntPair, MachineModel> {
			public LogRecordRangeGetter(MachineEvent successEvent) {
				super(successEvent);
			}

			@Override
			public void onSuccess0(IntPair result) {
				logRecordRange = result;
				idCtr = logRecordRange.i1 - 1;
				firstAddedThisBuffer = -1;
			}

			@Override
			public void start() {
				LogStore.get().getIdRange(this);
			}
		}

		class LogRecordRangeDeleter extends
				PersistenceCallbackTransitionHandler<Void, MachineModel> {
			public LogRecordRangeDeleter(MachineEvent successEvent) {
				super(successEvent);
			}

			@Override
			public void onSuccess0(Void result) {
			}

			@Override
			public void start() {
				IntPair range = new IntPair(firstAddedThisBuffer,
						lastAddedThisBuffer);
				if (range.isZero()) {
					onSuccess(null);
				} else {
					LogStore.get().removeIdRange(range, this);
				}
			}
		}

		class LogRecordCheckOnline extends
				AsyncCallbackTransitionHandler<Void, MachineModel> {
			public LogRecordCheckOnline(MachineEvent successEvent) {
				super(successEvent);
			}

			@Override
			public void onSuccess0(Void result) {
				maybeOffline = false;
			}

			@Override
			public void start() {
				if (!maybeOffline) {
					onSuccess(null);
					return;
				}
				if (buffer.length() == 0) {
					onSuccess(null);
					return;
				} else {
					CommonRemoteServiceAsync async = ClientLayerLocator.get()
							.getCommonRemoteServiceAsyncProvider()
							.getServiceInstance();
					async.ping(this);
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				handleExpectableMaybeOffline(caught);
			}
		}

		class LogRecordRemotePusher extends
				AsyncCallbackTransitionHandler<Void, MachineModel> {
			public LogRecordRemotePusher(MachineEvent successEvent) {
				super(successEvent);
			}

			@Override
			public void onSuccess0(Void result) {
				maybeOffline = false;
			}

			@Override
			public void start() {
				if (buffer.length() == 0) {
					onSuccess(null);
				} else {
					CommonRemoteServiceAsync async = ClientLayerLocator.get()
							.getCommonRemoteServiceAsyncProvider()
							.getServiceInstance();
					async.logClientRecords(buffer.toString(), this);
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				handleExpectableMaybeOffline(caught);
			}
		}

		class LogRecordGetter
				extends
				PersistenceCallbackTransitionHandler<Map<Integer, String>, MachineModel> {
			public LogRecordGetter(MachineEvent successEvent) {
				super(successEvent);
			}

			@Override
			public void onSuccess(Map<Integer, String> result) {
				if (!result.isEmpty()) {
					if (firstAddedThisBuffer == -1) {
						firstAddedThisBuffer = idCtr;
					}
					lastAddedThisBuffer = idCtr;
					buffer.append(result.values().iterator().next());
					buffer.append("\n");
					if (buffer.length() < PREFERRED_MAX_PUSH_SIZE
							&& idCtr < logRecordRange.i2) {
						model.getMachine().newEvent(keepAdding);
						return;
					}
				}
				model.getMachine().newEvent(successEvent);
			}

			@Override
			public void onSuccess0(Map<Integer, String> result) {
				// two possible emergent arrows, so no default
			}

			@Override
			public void start() {
				++idCtr;
				LogStore.get().getRange(idCtr, idCtr, this);
			}
		}

		public void handleExpectableMaybeOffline(Throwable caught) {
			if (ClientUtils.maybeOffline(caught)) {
				maybeOffline = true;
				machine.clear();
				machine = null;
			} else {
				throw new WrappedRuntimeException(caught);
			}
		}
	}

	private RemoteLogPersisterMachine machine = null;

	int pushCount = 0;

	// get last uploaded, get range, push(http), delete, update lastuploaded,
	public synchronized void push() {
		pushCount++;
		if (machine == null) {
			pushCount--;
			machine = new RemoteLogPersisterMachine();
			machine.addListener(MachineState.END, null,
					new MachineListener<MachineModel>() {
						@Override
						public void beforeAction(MachineModel model) {
						}

						@Override
						public void afterAction(MachineModel model) {
							machine = null;
						}
					});
			machine.start();
		}
	}
}
