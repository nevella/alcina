package cc.alcina.framework.common.client.state;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.IsClassFilter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Manages an ecology of players - a sort of organic state machine
 * 
 * @author nick@alcina.cc
 * 
 */
public class Consort<D, S> {
	private static final String PLAYERS_WITH_EQUAL_DEPS_ERR = "Players with equal"
			+ " dependencies and priorities: \n%s\n%s";

	public static final transient String BEFORE_PLAY = "BEFORE_PLAY";

	public static final transient String AFTER_PLAY = "AFTER_PLAY";

	public static final transient String STATES = "STATES";

	public static final transient String ERROR = "ERROR";

	public static final transient String FINISHED = "FINISHED";

	private TopicPublisher topicPublisher = new TopicPublisher();

	LinkedList<Player<D>> players = new LinkedList<Player<D>>();

	LinkedList<Player<D>> removed = new LinkedList<Player<D>>();

	Map<S, ConsortSignalHandler<S>> signalHandlers = new LinkedHashMap<S, ConsortSignalHandler<S>>();

	private Player currentPlayer = null;

	private boolean consumingQueue;

	private boolean running;

	private boolean trace;

	private boolean simulate;

	private int playedCount = 0;

	private int indent = 0;

	private Set<D> reachedStates = new LinkedHashSet<D>();

	Player replayPlayer = null;

	private Consort parent;

	public void addEndpointPlayer() {
		addEndpointPlayer(null);
	}

	public void addEndpointPlayer(AsyncCallback completionCallback) {
		D lastRequired = CommonUtils.last(players).getProvides().iterator()
				.next();
		addPlayer(new EndpointPlayer(lastRequired, completionCallback));
	}

	public void addIfNotMember(Player player) {
		if (getTaskForClass(player.getClass()) == null) {
			addPlayer(player);
		}
	}

	public <T extends Player<D>> T addPlayer(T player) {
		player.setConsort(this);
		players.addLast(player);
		return player;
	}

	public void addSignalHandler(ConsortSignalHandler<S> signal) {
		if (signalHandlers.containsKey(signal.handlesSignal())) {
			throw new RuntimeException("Duplicate signal handlers for "
					+ signal.handlesSignal());
		}
		signalHandlers.put(signal.handlesSignal(), signal);
	}

	public void cancel() {
		running = false;
		if (currentPlayer instanceof ConsortPlayer) {
			((ConsortPlayer) currentPlayer).getStateConsort().cancel();
		}
		currentPlayer = null;
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
		reachedStates.clear();
	}

	public boolean containsState(D state) {
		return reachedStates.contains(state);
	}

	public <P extends Player> P getTaskForClass(Class<P> clazz) {
		return (P) CollectionFilters.first(players, new IsClassFilter(clazz));
	}

	public void finished() {
		running = false;
		topicPublisher.publishTopic(FINISHED, null);
	}

	public boolean isRunning() {
		return this.running;
	}

	public boolean isSimulate() {
		return this.simulate;
	}

	public boolean isTrace() {
		return this.trace;
	}

	public void listenerDelta(String key, TopicListener listener, boolean add) {
		topicPublisher.listenerDelta(key, listener, add);
	}

	public void nudge() {
		consumeQueue();
	}

	public void onFailure(Throwable throwable) {
		topicPublisher.publishTopic(ERROR, throwable);
		throw new WrappedRuntimeException(throwable);
	}

	public void removeStates(Collection<D> states) {
		reachedStates.removeAll(states);
	}

	public void replay(Player player) {
		assert player instanceof LoopingPlayer;
		replayPlayer = player;
		currentPlayer = null;
		if (consumingQueue) {
		} else {
			consumeQueue();
		}
	}

	public void restart() {
		clearReachedStates();
		players.addAll(removed);
		removed.clear();
		replayPlayer = null;
		start();
	}

	public void setSimulate(boolean simulate) {
		this.simulate = simulate;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}

	public void signal(S signal) {
		signalHandlers.get(signal).signal(this);
	}

	public void start() {
		running = true;
		playedCount = 0;
		consumeQueue();
	}

	public void wasPlayed(Player<D> player) {
		wasPlayed(player, player.getProvides());
	}

	public void wasPlayed(Player<D> player, Collection<D> resultantStates) {
		playedCount++;
		currentPlayer = null;
		// TODO - warn if resultantstates >1 and a non-parallel consort?
		if (reachedStates.addAll(resultantStates)) {
			publishTopicWithBubble(STATES, null);
			if (isTrace()) {
				System.out.println(CommonUtils.formatJ("%s     [%s]",
						CommonUtils.padStringLeft("", indent, '\t'),
						CommonUtils.join(resultantStates, ", ")));
			}
		}
		publishTopicWithBubble(AFTER_PLAY, player);
		consumeQueue();
	}

	protected void publishTopicWithBubble(String key, Object message) {
		topicPublisher.publishTopic(key, message);
		if (parent != null) {
			parent.publishTopicWithBubble(key, message);
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
		Set<D> providerDependencies = new LinkedHashSet<D>();
		Set<Player> addedDependencies = new LinkedHashSet<Player>();
		Player lastAdded = null;
		boolean hasNonProviders = false;
		while (true) {
			Set<D> seenDependencies = new LinkedHashSet<D>();
			for (Player<D> player : players) {
				if (isActive(player)) {
					hasNonProviders |= player.getProvides().isEmpty();
					if (satisfiesDeps(player, providerDependencies)) {
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
								if (!player.isAllowEqualPriority()
										|| result.isAllowEqualPriority()) {
									throw new RuntimeException(
											CommonUtils
													.formatJ(
															PLAYERS_WITH_EQUAL_DEPS_ERR,
															player, result));
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
				if (isTrace() && (playedCount == 0 || hasNonProviders)) {
					if (players.size() > 0) {
						Player missed = lastAdded != null ? lastAdded : players
								.iterator().next();
						System.out.println(CommonUtils.formatJ(
								"Unable to resolve dependencies: %s\n\t%s",
								missed.getRequires(), missed));
						int j = 3;
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

	protected void consumeQueue() {
		if (currentPlayer != null || consumingQueue || !running) {
			return;
		}
		consumingQueue = true;
		// this means that synchronous players will be dispatched sequentially
		// within the while loop, but async tasks will be dispatched by
		// (non-recursive) consumeQueue/wasPlayed calls
		// also allow shortcut for looping tasks
		while (currentPlayer == null && running) {
			boolean replaying = replayPlayer != null;
			Player<D> player = replaying ? replayPlayer : nextPlayer();
			replayPlayer = null;
			if (player != null) {
				maybeRemovePlayersFromQueue(player);
				currentPlayer = player;
				executePlayer(player, replaying);
			} else {
				break;
			}
		}
		consumingQueue = false;
	}

	protected void executePlayer(Player<D> player, boolean replaying) {
		if (isTrace()) {
			System.out.println(CommonUtils.formatJ("%s%s -> %s",
					CommonUtils.padStringLeft("", indent, '\t'),
					CommonUtils.simpleClassName(getClass()),
					CommonUtils.simpleClassName(player.getClass())));
			if (player instanceof ConsortPlayer) {
				Consort stateConsort = ((ConsortPlayer) player)
						.getStateConsort();
				if (stateConsort != null) {
					stateConsort.indent = indent + 1;
					stateConsort.setTrace(true);
					stateConsort.setSimulate(isSimulate());
				}
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

	protected int getRelativePriority(Player<D> player1, Player<D> player2) {
		return player1.getPriority() - player2.getPriority();
	}

	Set<D> getReachedStates() {
		return this.reachedStates;
	}

	public Consort getParent() {
		return this.parent;
	}

	public void setParent(Consort parent) {
		this.parent = parent;
	}

	public void deferredRemove(final String key, final TopicListener listener) {
		Registry.impl(TimerWrapperProvider.class).scheduleDeferred(
				new Runnable() {
					@Override
					public void run() {
						listenerDelta(key, listener, false);
					}
				});
	}
}
