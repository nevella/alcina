package cc.alcina.framework.common.client.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.IsClassFilter;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

/**
 * Manages an ecology of players - a sort of organic state machine.
 * 
 * 
 * The usage as per: noun noun: consort; plural noun: consorts a small group of
 * musicians performing together, typically playing instrumental music of the
 * Renaissance period. "a consort of viols"
 * 
 * @author nick@alcina.cc
 * 
 */
public class Consort<D> {
	private static final String PLAYERS_WITH_EQUAL_DEPS_ERR = "Players with equal"
			+ " dependencies and priorities: \n%s\n%s\n  - deps: %s";

	public static final transient String BEFORE_PLAY = "BEFORE_PLAY";

	public static final transient String AFTER_PLAY = "AFTER_PLAY";

	public static final transient String STATES = "STATES";

	public static final transient String ERROR = "ERROR";

	public static final transient String FINISHED = "FINISHED";

	public static final transient String CANCELLED = "CANCELLED";

	public static final transient String NO_ACTIVE_PLAYERS = "NO_ACTIVE_PLAYERS";

	protected static final String IGNORE_PLAYED_STATES_IF_NOT_CONTAINED = Consort.class
			.getName() + ".IGNORE_PLAYED_STATES_IF_NOT_CONTAINED";

	private TopicPublisher topicPublisher = new TopicPublisher();

	protected LinkedList<Player<D>> players = new LinkedList<Player<D>>();

	LinkedList<Player<D>> removed = new LinkedList<Player<D>>();

	protected LinkedList<Player<D>> playing = new LinkedList<Player<D>>();

	private boolean consumingQueue;

	private boolean running;

	private boolean simulate;

	private int playedCount = 0;

	private Set<D> reachedStates = new LightSet<D>();

	Player replayPlayer = null;

	private Consort parentConsort;

	private boolean synchronous;

	protected ParallelArbiter parallelArbiter;

	private boolean throwOnUnableToResolveDependencies;

	protected Logger metricLogger = AlcinaLogUtils.getMetricLogger(getClass());

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private String lastInfoLogMessage = null;

	public void addEndpointPlayer() {
		addEndpointPlayer(null, true);
	}

	public void addEndpointPlayer(AsyncCallback completionCallback,
			boolean finishes) {
		D lastRequired = CommonUtils.last(players).getProvides().iterator()
				.next();
		addPlayer(
				new EndpointPlayer(lastRequired, completionCallback, finishes));
	}

	public void addIfNotMember(Player player) {
		for (Player p : getTasksForClass(player.getClass())) {
			if (p.getProvides().equals(player.getProvides())
					&& p.getRequires().equals(player.getRequires())) {
				return;
			}
		}
		addPlayer(player);
	}

	public OneTimeFinishedAsyncCallbackAdapter
			addOneTimeFinishedCallback(AsyncCallback finishedCallback) {
		if (finishedCallback != null) {
			return new OneTimeFinishedAsyncCallbackAdapter(finishedCallback);
		} else {
			return null;
		}
	}

	public OneTimeFinishedAsyncCallbackAdapter addOneTimeStateCallback(D state,
			AsyncCallback stateCallback) {
		if (stateCallback != null) {
			return new OneTimeFinishedAsyncCallbackAdapter(stateCallback,
					state);
		} else {
			return null;
		}
	}

	public <T extends Player> T addPlayer(T player) {
		player.setConsort(this);
		players.addLast(player);
		return player;
	}

	public StateListenerWrapper addStateListener(TopicListener listener,
			D state) {
		StateListenerWrapper wrapper = new StateListenerWrapper(listener,
				state);
		topicPublisher.listenerDelta(STATES, wrapper, true);
		wrapper.fireIfExisting();
		return wrapper;
	}

	public void cancel() {
		running = false;
		if (playing != null) {
			for (Player player : playing) {
				if (player instanceof ConsortPlayer) {
					Consort stateConsort = ((ConsortPlayer) player)
							.getStateConsort();
					if (stateConsort != null) {
						stateConsort.cancel();
					}
				}
				if (player != null) {
					player.cancel();
				}
			}
		}
		playing.clear();
	}

	public void clear() {
		CollectionFilter<Player> isCancellableFilter = new CollectionFilter<Player>() {
			@Override
			public boolean allow(Player o) {
				return !o.isCancellable();
			}
		};
		CollectionFilters.filterInPlace(players, isCancellableFilter);
		clearReachedStates();
	}

	public void clearReachedStates() {
		removeStates(new ArrayList<D>(reachedStates));
	}

	public boolean containsState(D state) {
		return reachedStates.contains(state);
	}

	public void deferredRemove(final List<String> keys,
			final TopicListener listener) {
		Registry.impl(TimerWrapperProvider.class)
				.scheduleDeferred(new Runnable() {
					@Override
					public void run() {
						for (String key : keys) {
							listenerDelta(key, listener, false);
						}
					}
				});
	}

	public void doOrDefer(TopicListener topicListener, D state) {
		if (containsState(state)) {
			topicListener.topicPublished(null, state);
		} else {
			addStateListener(topicListener, state);
		}
	}

	public void exitListenerDelta(TopicListener listener,
			boolean includeNoActivePlayers, boolean add) {
		if (add) {
			listenerDelta(Consort.CANCELLED, listener, true);
			listenerDelta(Consort.ERROR, listener, true);
			listenerDelta(Consort.FINISHED, listener, true);
			if (includeNoActivePlayers) {
				listenerDelta(Consort.NO_ACTIVE_PLAYERS, listener, true);
			}
		} else {
			deferredRemove(
					Arrays.asList(Consort.CANCELLED, Consort.ERROR,
							Consort.FINISHED, Consort.NO_ACTIVE_PLAYERS),
					listener);
		}
	}

	public void finished() {
		running = false;
		logger.info(Ax.format("%s     [%s]",
				CommonUtils.padStringLeft("", depth(), '\t'),
				"----CONSORT FINISHED"));
		topicPublisher.publishTopic(FINISHED, null);
	}

	public Consort getParentConsort() {
		return this.parentConsort;
	}

	public <P extends Player> List<P> getTasksForClass(Class<P> clazz) {
		return (List) CollectionFilters.filter(players,
				new IsClassFilter(clazz));
	}

	public boolean isRunning() {
		return this.running;
	}

	public boolean isSimulate() {
		return this.simulate;
	}

	public boolean isSynchronous() {
		return this.synchronous;
	}

	public boolean isThrowOnUnableToResolveDependencies() {
		return this.throwOnUnableToResolveDependencies;
	}

	public void listenerDelta(String key, TopicListener listener, boolean add) {
		topicPublisher.listenerDelta(key, listener, add);
	}

	public void nudge() {
		running = true;
		consumeQueue();
	}

	public void onFailure(Throwable throwable) {
		running = false;
		Ax.simpleExceptionOut(throwable);
		topicPublisher.publishTopic(ERROR, throwable);
		throw new WrappedRuntimeException(throwable);
	}

	public void passLoggersAndFlagsToChild(Consort child) {
		child.metricLogger = metricLogger;
		child.logger = logger;
		child.setSimulate(isSimulate());
	}

	public void removeStates(Collection<D> states) {
		logger.info(Ax.format("%s rmv:[%s]",
				CommonUtils.padStringLeft("", depth(), '\t'),
				CommonUtils.join(states, ", ")));
		modifyStates(states, false);
	}

	public void replay(Player player) {
		assert player instanceof LoopingPlayer;
		replayPlayer = player;
		playing.clear();
		if (consumingQueue) {
		} else {
			consumeQueue();
		}
	}

	public void restart() {
		clearReachedStates();
		players.addAll(removed);
		removed.clear();
		playing.clear();
		replayPlayer = null;
		start();
	}

	public void runWhenFinished(AsyncCallback finishedCallback) {
		if (!running) {
			finishedCallback.onSuccess(null);
		} else {
			addOneTimeFinishedCallback(finishedCallback);
		}
	}

	public void setParentConsort(Consort parent) {
		this.parentConsort = parent;
	}

	public void setSimulate(boolean simulate) {
		this.simulate = simulate;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public void setThrowOnUnableToResolveDependencies(
			boolean throwOnUnableToResolveDependencies) {
		this.throwOnUnableToResolveDependencies = throwOnUnableToResolveDependencies;
	}

	public void start() {
		logger.info("Starting consort - {}", this);
		running = true;
		playedCount = 0;
		consumeQueue();
	}

	// note - listener must be actually TopicListener<StatesDelta> - but GWT
	// doesn't like that for Consort subclasses
	public void statesListenerDelta(TopicListener listener, boolean add) {
		topicPublisher.listenerDelta(STATES, listener, add);
	}

	public void wasPlayed(Player<D> player) {
		wasPlayed(player, player.getProvides());
	}

	public void wasPlayed(Player<D> player, Collection<D> resultantStates) {
		wasPlayed(player, resultantStates, true);
	}

	// FIXME - applifecycle.consort - cleanup - this works - unless we want some
	// sort of threaded
	// queue/consumer model - but it ain't so pretty
	//
	public void wasPlayed(Player<D> player, Collection<D> resultantStates,
			boolean keepGoing) {
		if (!isRunning()) {
			return;
		}
		if (!playing.contains(player)) {
			if (LooseContext.is(IGNORE_PLAYED_STATES_IF_NOT_CONTAINED)) {
				return;
			}
		}
		playedCount++;
		assert playing.contains(player);
		playing.remove(player);
		// FIXME - applifecycle.consort - warn if resultantstates >1 and a
		// non-parallel consort?
		modifyStates(resultantStates, true);
		metricLogger.debug(Ax.format("%s     %s: %s ms",
				CommonUtils.padStringLeft("", depth(), '\t'),
				player.shortName(),
				System.currentTimeMillis() - player.getStart()));
		publishTopicWithBubble(AFTER_PLAY, player);
		if (keepGoing) {
			consumeQueue();
		}
	}

	private void consumeQueue0() {
		if (!canAddPlayers() || consumingQueue || !running) {
			return;
		}
		consumingQueue = true;
		// this means that synchronous players will be dispatched sequentially
		// within the while loop, but async tasks will be dispatched by
		// (non-recursive) consumeQueue/wasPlayed calls
		// also allow shortcut for looping tasks
		while (canAddPlayers() && running) {
			boolean replaying = replayPlayer != null;
			Player<D> player = replaying ? replayPlayer : nextPlayer();
			replayPlayer = null;
			if (player != null) {
				maybeRemovePlayersFromQueue(player);
				if (!playing.contains(player)) {
					playing.add(player);
				}
				executePlayer(player, replaying);
			} else {
				break;
			}
		}
		consumingQueue = false;
		if (playing.isEmpty() && running) {
			topicPublisher.publishTopic(NO_ACTIVE_PLAYERS, null);
		}
	}

	private boolean isActive(Player<D> player) {
		return reachedStates.containsAll(player.getPreconditions());
	}

	private void maybeRemovePlayersFromQueue(Player<D> player) {
		// to handle jadeClientState use cases - but think this can be done
		// better via signals?
		// if (player.isPerConsortSingleton()) {
		// CollectionFilters.filterInPlace(players, new InverseFilter(
		// new IsClassFilter(player.getClass())));
		// } else {
		// players.remove(player);
		// }
		if (player.isRemoveAfterPlay()) {
			removed.add(player);
			players.remove(player);
		}
	}

	private void modifyStates(Collection<D> states, boolean add) {
		LightSet<D> reachedCopy = new LightSet<D>(reachedStates);
		boolean mod = add ? reachedStates.addAll(states)
				: reachedStates.removeAll(states);
		if (mod) {
			publishTopicWithBubble(STATES,
					new StatesDelta(reachedCopy, reachedStates));
			logger.debug(Ax.format("%s     [%s]",
					CommonUtils.padStringLeft("", depth(), '\t'),
					CommonUtils.join(states, ", ")));
		}
	}

	private Player<D> nextPlayer() {
		Player<D> result = null;
		// dependecy pass
		Multimap<Object, List<Player>> satisfiers = new Multimap<Object, List<Player>>();
		for (Player player : players) {
			if (isActive(player) && player.getProvides().size() > 0) {
				for (Object provides : player.getProvides()) {
					satisfiers.add(provides, player);
				}
			}
		}
		int lastCheckedCount = -1;
		Set<D> providerDependencies = new LightSet<D>();
		Set<Player> addedDependencies = new LightSet<Player>();
		Player lastAdded = null;
		boolean hasNonProviders = false;
		while (true) {
			Set<D> seenDependencies = new LightSet<D>();
			for (Player<D> player : players) {
				if (playing.contains(player)) {
					continue;
				}
				if (isActive(player)) {
					hasNonProviders |= player.getProvides().isEmpty();
					if (satisfiesDeps(player, providerDependencies)) {
						if (playing.size() > 0) {
							if (!parallelArbiter.allow(player)) {
								continue;
							}
						}
						if (result == null) {
							result = player;
						} else {
							int relPriority = getRelativePriority(player,
									result);
							if (relPriority > 0) {
								result = player;
							} else if (relPriority < 0) {
								// keep current;
							} else {
								if (!(player.isAllowEqualPriority()
										|| result.isAllowEqualPriority())) {
									throw new RuntimeException(Ax.format(
											PLAYERS_WITH_EQUAL_DEPS_ERR, player,
											result, providerDependencies));
								}
							}
						}
					} else {
						if (satisfiesSomeSoughtDependenciesOrIsNotASatisfier(
								player, providerDependencies)) {
							if (addedDependencies.add(player)) {
								lastAdded = player;
							}
							seenDependencies.addAll(player.getRequires());
						}
					}
				}
			}
			if (result != null) {
				break;
			}
			providerDependencies.addAll(seenDependencies);
			providerDependencies.removeAll(reachedStates);
			if (providerDependencies.size() == lastCheckedCount) {
				if (playedCount == 0 || hasNonProviders) {
					if (players.size() > 0 && playing.isEmpty()) {
						Player missed = lastAdded != null ? lastAdded
								: players.iterator().next();
						String message = Ax.format(
								"Unable to resolve dependencies: %s\n\t%s",
								missed.getRequires(), missed);
						logger.info(message);
						if (isThrowOnUnableToResolveDependencies()) {
							onFailure(new RuntimeException(message));
						}
					}
				}
				break;
			}
			lastCheckedCount = providerDependencies.size();
		}
		return result;
	}

	/*
	 * In first pass, just go for immediately satisfied, non-state-provider
	 * players
	 * 
	 * In the second, we look for state-providers in an expanding loop, (for
	 * dependency chains), looking for any path forward
	 */
	private boolean satisfiesDeps(Player<D> player,
			Collection<D> providerDependencies) {
		return satisfiesSomeSoughtDependenciesOrIsNotASatisfier(player,
				providerDependencies)
				&& reachedStates.containsAll(player.getRequires());
	}

	private boolean satisfiesSomeSoughtDependenciesOrIsNotASatisfier(
			Player<D> player, Collection<D> providerDependencies) {
		if (player.getProvides().isEmpty()) {
			return true;
		}
		// make sure this player hasn't already provided a state
		return CommonUtils.intersection(player.getProvides(), reachedStates)
				.isEmpty()
				&& CommonUtils.intersection(player.getProvides(),
						providerDependencies).size() > 0;
	}

	protected void addState(D state) {
		// only do on startup - no logging
		modifyStates(Collections.singletonList(state), true);
	}

	protected void addStates(Collection<D> states) {
		logger.info(Ax.format("%s add:[%s]",
				CommonUtils.padStringLeft("", depth(), '\t'),
				CommonUtils.join(states, ", ")));
		modifyStates(states, true);
	}

	protected void consumeQueue() {
		try {
			consumeQueue0();
		} catch (Throwable e) {
			onFailure(e);
		}
	}

	protected int depth() {
		Consort cursor = this;
		int depth = 0;
		while (cursor.getParentConsort() != null) {
			depth++;
			cursor = cursor.getParentConsort();
		}
		return depth;
	}

	protected void executePlayer(Player<D> player, boolean replaying) {
		String message = Ax.format("%s%s%s -> %s",
				(playing.size() == 1 ? "    "
						: Ax.format("[%s] ", playing.size())),
				CommonUtils.padStringLeft("", depth(), "    "),
				CommonUtils.simpleClassName(getClass()),
				player.provideNameForTransitions());
		if (!CommonUtils.equalsWithNullEmptyEquality(message,
				lastInfoLogMessage)) {
			logger.info(message);
			lastInfoLogMessage = message;
		}
		if (player instanceof ConsortPlayer) {
			Consort stateConsort = ((ConsortPlayer) player).getStateConsort();
			if (stateConsort != null) {
				passLoggersAndFlagsToChild(stateConsort);
			}
		}
		if (isSimulate()) {
			if (player instanceof ConsortPlayer) {
				Consort stateConsort = ((ConsortPlayer) player)
						.getStateConsort();
				if (stateConsort != null) {
					((ConsortPlayer) player).getStateConsort().start();
				}
			}
			wasPlayed(player);
		} else {
			publishTopicWithBubble(BEFORE_PLAY, player);
			player.play(replaying);
		}
	}

	protected Set<D> getReachedStates() {
		return this.reachedStates;
	}

	protected int getRelativePriority(Player<D> player1, Player<D> player2) {
		return player1.getPriority() - player2.getPriority();
	}

	protected void publishTopicWithBubble(String key, Object message) {
		topicPublisher.publishTopic(key, message);
		if (parentConsort != null) {
			parentConsort.publishTopicWithBubble(key, message);
		}
	}

	boolean canAddPlayers() {
		return playing.isEmpty() || parallelArbiter != null;
	}

	public class OneTimeFinishedAsyncCallbackAdapter implements TopicListener {
		private AsyncCallback callback;

		private D state;

		public OneTimeFinishedAsyncCallbackAdapter(AsyncCallback callback) {
			this.callback = callback;
			exitListenerDelta(this, true, true);
		}

		public OneTimeFinishedAsyncCallbackAdapter(AsyncCallback callback,
				D state) {
			this(callback);
			this.state = state;
			listenerDelta(STATES, this, true);
		}

		@Override
		public void topicPublished(String key, Object message) {
			boolean remove = false;
			try {
				if (key == ERROR) {
					remove = true;
					callback.onFailure((Throwable) message);
				} else {
					if (key == FINISHED || key == NO_ACTIVE_PLAYERS
							|| key == CANCELLED) {
						if (state == null) {
							remove = true;
							callback.onSuccess(message);
						}
					} else {
						if (key == STATES) {
							StatesDelta statesDelta = (StatesDelta) message;
							if (statesDelta.wasStateAdded(state)) {
								remove = true;
								callback.onSuccess(message);
							}
						}
					}
				}
			} finally {
				if (remove) {
					exitListenerDelta(this, true, false);
					deferredRemove(Arrays.asList(STATES), this);
				}
			}
		}
	}

	public class StatesDelta {
		public Set<D> oldValue;

		public Set<D> newValue;

		public StatesDelta(Set<D> oldValue, Set<D> newValue) {
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public boolean wasStateAdded(D value) {
			return !oldValue.contains(value) && newValue.contains(value);
		}

		public boolean wasStateRemoved(D value) {
			return oldValue.contains(value) && !newValue.contains(value);
		}
	}

	class StateListenerWrapper implements TopicListener<StatesDelta> {
		private TopicListener delegate;

		private D state;

		public StateListenerWrapper(TopicListener delegate, D state) {
			this.delegate = delegate;
			this.state = state;
		}

		public void fireIfExisting() {
			Registry.impl(TimerWrapperProvider.class)
					.scheduleDeferred(new Runnable() {
						@Override
						public void run() {
							StatesDelta delta = new StatesDelta(
									Collections.EMPTY_SET, reachedStates);
							topicPublished(null, delta);
						}
					});
		}

		@Override
		public void topicPublished(String key, StatesDelta message) {
			if (message.wasStateAdded(state)) {
				delegate.topicPublished(STATES, state);
			}
		}
	}
}
