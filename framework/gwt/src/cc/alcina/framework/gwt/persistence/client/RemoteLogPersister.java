package cc.alcina.framework.gwt.persistence.client;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.state.EnumMachine;
import cc.alcina.framework.common.client.state.MachineEvent;
import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;
import cc.alcina.framework.common.client.state.MachineListener;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineState;
import cc.alcina.framework.common.client.state.MachineTransitionHandler;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.state.AsyncCallbackTransitionHandler;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.Lzw;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * This class deliberately doesn't listen for other classes
 * (committoremotetransform...) offline/online events - if we go online for some
 * reason, that's probably going to be logged and cause a push anyway
 * 
 * @author nick@alcina.cc
 * 
 */
public class RemoteLogPersister {
	private enum State {
		CHECK_ONLINE, GET_LOG_RECORD_RANGE, ADD_RECORDS, PUSH, DELETE
	}

	public static int PREFERRED_MAX_PUSH_SIZE = 30000;// bytes

	public boolean maybeOffline;

	class RemoteLogPersisterMachine extends EnumMachine<State, MachineModel> {
		IntPair logRecordRange = null;

		int idCtr = 0;

		int firstAddedThisBuffer = 0;

		int lastAddedThisBuffer = 0;

		private MachineEventImpl keepAdding;

		private StringBuilder buffer = new StringBuilder();

		public RemoteLogPersisterMachine() {
			super();
			init(State.class);
			buffer = new StringBuilder();
			EnumMachineState<State> addingState = getStateFor(State.ADD_RECORDS);
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
			MachineEvent jumpToEnd = new MachineEventImpl("checkOnline-end",
					getStateFor(State.CHECK_ONLINE), MachineState.END);

			public LogRecordCheckOnline(MachineEvent successEvent) {
				super(successEvent);
			}

			boolean rqRun = false;

			@Override
			public void onSuccess0(Void result) {
				maybeOffline = false;
				maybeUnmute();
			}

			public void maybeUnmute() {
				if (rqRun) {
					deferredUnmute();
				}
			}

			@Override
			public void start() {
				rqRun = false;
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
					rqRun = true;
					AlcinaTopics.muteStatisticsLogging(true);
					async.ping(this);
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				maybeUnmute();
				handleExpectableMaybeOffline(caught, jumpToEnd);
			}
		}

		class LogRecordRemotePusher extends
				AsyncCallbackTransitionHandler<Void, MachineModel> {
			MachineEvent jumpToEnd = new MachineEventImpl("push-end",
					getStateFor(State.PUSH), MachineState.END);

			private boolean rqRun;

			public LogRecordRemotePusher(MachineEvent successEvent) {
				super(successEvent);
			}

			@Override
			public void onSuccess0(Void result) {
				if (rqRun) {
					deferredUnmute();
					maybeOffline = false;
				}
			}

			@Override
			public void start() {
				if (buffer.length() == 0) {
					rqRun = false;
					onSuccess(null);
				} else {
					CommonRemoteServiceAsync async = ClientLayerLocator.get()
							.getCommonRemoteServiceAsyncProvider()
							.getServiceInstance();
					rqRun = true;
					AlcinaTopics.muteStatisticsLogging(true);
					async.logClientRecords(buffer.toString(), this);
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				deferredUnmute();
				handleExpectableMaybeOffline(caught, jumpToEnd);
			}
		}

		public void deferredUnmute() {
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					AlcinaTopics.muteStatisticsLogging(false);
				}
			});
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
					String value = result.values().iterator().next();
					if (value.startsWith("lzwb:")) {
						try {
							LogStore.get().setMuted(true);
							String dec = new Lzw().decompress(new String(
									Base64Utils.fromBase64(value.substring(5)),
									"UTF-8"));
							value = dec != null ? dec : value;
						} catch (UnsupportedEncodingException e) {
							throw new WrappedRuntimeException(e);
						} finally {
							LogStore.get().setMuted(false);
						}
					}
					buffer.append(value);
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

		public void handleExpectableMaybeOffline(Throwable caught,
				MachineEvent jumpToEnd) {
			if (ClientUtils.maybeOffline(caught)) {
				machine.newEvent(jumpToEnd);
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
			pushCount = 0;
			machine = new RemoteLogPersisterMachine();
			final long start = System.currentTimeMillis();
			machine.addListener(MachineState.END, null,
					new MachineListener<MachineModel>() {
						@Override
						public void beforeAction(MachineModel model) {
						}

						@Override
						public void afterAction(MachineModel model) {
							boolean pushAgain = machine.lastAddedThisBuffer < machine.logRecordRange.i2;
							long waitTime = (System.currentTimeMillis() - start) / 2;
							machine.clear();
							machine = null;
							if ((pushCount > 0 || pushAgain) && !maybeOffline) {
								TimerWrapper timer = ClientLayerLocator.get()
										.timerWrapperProvider()
										.getTimer(new Runnable() {
											@Override
											public void run() {
												push();
											}
										});
								timer.scheduleSingle(waitTime);
							}
						}
					});
			machine.start();
		}
	}
}
