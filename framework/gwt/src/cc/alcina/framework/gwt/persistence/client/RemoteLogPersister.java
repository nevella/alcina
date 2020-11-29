package cc.alcina.framework.gwt.persistence.client;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.EnumPlayer.EnumRunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.state.LoopingPlayer;
import cc.alcina.framework.common.client.state.Player;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.Lzw;

/**
 * This class deliberately doesn't listen for other classes
 * (committoremotetransform...) offline/online events - if we go online for some
 * reason, that's probably going to be logged and cause a push anyway
 * 
 * @author nick@alcina.cc
 * 
 */
public class RemoteLogPersister {
	public static int PREFERRED_MAX_PUSH_SIZE = 30000;// bytes

	public boolean maybeOffline;

	private RemoteLogPersisterConsort consort = null;

	int pushCount = 0;

	private TopicListener consortFinishedListener = new TopicListener() {
		@Override
		public void topicPublished(String key, Object message) {
			boolean pushAgain = consort.lastAddedThisBuffer < consort.logRecordRange.i2;
			long waitTime = (System.currentTimeMillis() - consortStart) / 2;
			consort.clear();
			consort = null;
			if ((pushCount > 0 || pushAgain) && !maybeOffline) {
				TimerWrapper timer = Registry.impl(TimerWrapperProvider.class)
						.getTimer(new Runnable() {
							@Override
							public void run() {
								push();
							}
						});
				timer.scheduleSingle(waitTime);
			}
		}
	};

	private long consortStart;

	// get last uploaded, get range, push(http), delete, update lastuploaded,
	public synchronized void push() {
		pushCount++;
		if (consort == null) {
			pushCount = 0;
			consort = new RemoteLogPersisterConsort();
			consortStart = System.currentTimeMillis();
			consort.listenerDelta(Consort.FINISHED, consortFinishedListener,
					true);
			consort.start();
		}
	}

	private enum State {
		CHECKED_ONLINE, GOT_LOG_RECORD_RANGE, ADDED_RECORDS, PUSHED, DELETED
	}

	class RemoteLogPersisterConsort extends Consort<State> {
		IntPair logRecordRange = null;

		int idCtr = 0;

		int firstAddedThisBuffer = 0;

		int lastAddedThisBuffer = 0;

		private StringBuilder buffer = new StringBuilder();

		public RemoteLogPersisterConsort() {
			super();
			buffer = new StringBuilder();
			addPlayer(new LogRecordCheckOnline());
			addPlayer(new LogRecordRangeGetter());
			addPlayer(new LogRecordAdder());
			addPlayer(new LogRecordRemotePusher());
			addPlayer(new LogRecordRangeDeleter());
			addEndpointPlayer();
		}

		public void deferredUnmute() {
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					AlcinaTopics.muteStatisticsLogging(false);
				}
			});
		}

		public void handleExpectableMaybeOffline(Throwable caught,
				Player player) {
			if (ClientUtils.maybeOffline(caught)) {
				maybeOffline = true;
				consort.finished();
			} else {
				throw new WrappedRuntimeException(caught);
			}
		}

		class LogRecordAdder extends
				EnumRunnableAsyncCallbackPlayer<Map<Integer, String>, State>
				implements LoopingPlayer {
			public LogRecordAdder() {
				super(State.ADDED_RECORDS);
			}

			@Override
			public String describeLoop() {
				return "keep adding until we have PREFERRED_MAX_PUSH_SIZE";
			}

			@Override
			public void loop() {
				++idCtr;
				LogStore.get().getRange(idCtr, idCtr, this);
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
						consort.replay(this);
					} else {
						wasPlayed();
					}
				} else {
					if (logRecordRange.contains(idCtr)) {
						consort.replay(this);
					} else {
						wasPlayed();
					}
				}
			}

			@Override
			public void run() {
				loop();
			}
		}

		class LogRecordCheckOnline
				extends EnumRunnableAsyncCallbackPlayer<Void, State> {
			boolean rqRun = false;

			public LogRecordCheckOnline() {
				super(State.CHECKED_ONLINE);
			}

			public void maybeUnmute() {
				if (rqRun) {
					deferredUnmute();
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				maybeUnmute();
				handleExpectableMaybeOffline(caught, this);
			}

			@Override
			public void onSuccess(Void result) {
				maybeOffline = false;
				maybeUnmute();
				wasPlayed(State.CHECKED_ONLINE);
			}

			@Override
			public void run() {
				rqRun = false;
				if (!maybeOffline) {
					onSuccess(null);
					return;
				}
				if (buffer.length() == 0) {
					onSuccess(null);
					return;
				} else {
					CommonRemoteServiceAsync async = Registry
							.impl(CommonRemoteServiceAsync.class);
					rqRun = true;
					AlcinaTopics.muteStatisticsLogging(true);
					async.ping(this);
				}
			}
		}

		class LogRecordRangeDeleter
				extends EnumRunnableAsyncCallbackPlayer<Void, State> {
			public LogRecordRangeDeleter() {
				super(State.DELETED);
			}

			@Override
			public void run() {
				IntPair range = new IntPair(firstAddedThisBuffer,
						lastAddedThisBuffer);
				if (range.isZero()) {
					onSuccess(null);
				} else {
					LogStore.get().removeIdRange(range, this);
				}
			}
		}

		class LogRecordRangeGetter
				extends EnumRunnableAsyncCallbackPlayer<IntPair, State> {
			public LogRecordRangeGetter() {
				super(State.GOT_LOG_RECORD_RANGE);
			}

			@Override
			public void onSuccess(IntPair result) {
				logRecordRange = result;
				idCtr = logRecordRange.i1 - 1;
				firstAddedThisBuffer = -1;
				wasPlayed();
			}

			@Override
			public void run() {
				LogStore.get().getIdRange(this);
			}
		}

		class LogRecordRemotePusher
				extends EnumRunnableAsyncCallbackPlayer<Void, State> {
			private boolean rqRun;

			public LogRecordRemotePusher() {
				super(State.PUSHED);
			}

			@Override
			public void onFailure(Throwable caught) {
				deferredUnmute();
				handleExpectableMaybeOffline(caught, this);
			}

			@Override
			public void onSuccess(Void result) {
				if (rqRun) {
					deferredUnmute();
					maybeOffline = false;
				}
				wasPlayed(State.PUSHED);
			}

			@Override
			public void run() {
				if (buffer.length() == 0) {
					rqRun = false;
					onSuccess(null);
				} else {
					CommonRemoteServiceAsync async = Registry
							.impl(CommonRemoteServiceAsync.class);
					rqRun = true;
					AlcinaTopics.muteStatisticsLogging(true);
					async.logClientRecords(buffer.toString(), this);
				}
			}
		}
	}
}
