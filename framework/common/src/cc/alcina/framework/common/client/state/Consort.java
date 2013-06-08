package cc.alcina.framework.common.client.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.CollectionFilters.InverseFilter;
import cc.alcina.framework.common.client.collections.IsClassFilter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher;

/**
 * Manages an ecology of players - a sort of organic state machine
 * 
 * @author nick@alcina.cc
 * 
 */
public class Consort<D> {
	public static final transient String BEFORE_PLAY = "BEFORE_PLAY";

	public static final transient String AFTER_PLAY = "AFTER_PLAY";

	public static final transient String STATES = "STATES";

	private TopicPublisher topicPublisher = new TopicPublisher();

	LinkedList<Player<D>> players = new LinkedList<Player<D>>();

	public Collection<D> addPlayer(Player<D> player) {
		player.setConsort(this);
		players.addLast(player);
		consumeQueue();
		return player.resolveRequires();
	}

	private boolean executingPlayer = false;

	private boolean consumingQueue;

	private boolean running;

	private boolean trace;

	public void start() {
		running = true;
		consumeQueue();
	}

	public void clear() {
		CollectionFilter<Player> isCancellableFilter = new CollectionFilter<Player>() {
			@Override
			public boolean allow(Player o) {
				return !o.isCancellable();
			}
		};
		CollectionFilters.filterInPlace(players, isCancellableFilter);
		reachedStates.clear();
	}

	public void wasPlayed(Player<D> player) {
		executingPlayer = false;
		if (player.getProvides() != null) {
			reachedStates.addAll(player.getProvides());
		}
		topicPublisher.publishTopic(AFTER_PLAY, player);
		consumeQueue();
	}

	public void nudge() {
		consumeQueue();
	}

	protected void consumeQueue() {
		if (executingPlayer || consumingQueue || !running) {
			return;
		}
		consumingQueue = true;
		// this means that synchronous players will be dispatched sequentially
		// within the while loop, but async tasks will be dispatched by
		// (non-recursive) consumeQueue/wasPlayed calls
		while (!executingPlayer) {
			Player<D> player = nextPlayer();
			if (player != null) {
				maybeRemovePlayersFromQueue(player);
				executingPlayer = true;
				executePlayer(player);
			} else {
				break;
			}
		}
		consumingQueue = false;
	}

	protected void executePlayer(Player<D> player) {
		if (isTrace()) {
			System.out.println("Playing: "
					+ CommonUtils.simpleClassName(player.getClass()));
			wasPlayed(player);
		} else {
			topicPublisher.publishTopic(BEFORE_PLAY, player);
			player.play();
		}
	}

	private void maybeRemovePlayersFromQueue(Player<D> player) {
		if (player.isPerConsortSingleton()) {
			CollectionFilters.filterInPlace(players, new InverseFilter(
					new IsClassFilter(player.getClass())));
		} else {
			players.remove(player);
		}
		if (!player.isRemoveAfterPlay()) {
			players.add(player);
		}
	}

	private Set<D> reachedStates = new LinkedHashSet<D>();

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
		while (true) {
			Set<D> seenDependencies = new LinkedHashSet<D>();
			for (Player<D> player : players) {
				if (isActive(player)) {
					if (satisfiesDeps(player, providerDependencies)) {
						if (result == null || hasHigherPriority(player, result)) {
							result = player;
						}
					} else {
						if (satisfiesSomeSoughtDependenciesOrIsNotASatisfier(
								player, providerDependencies)) {
							seenDependencies.addAll(player.resolveRequires());
						}
					}
				}
			}
			if (result != null) {
				break;
			}
			providerDependencies.addAll(seenDependencies);
			if (providerDependencies.size() == lastCheckedCount) {
				break;
			}
			lastCheckedCount = providerDependencies.size();
		}
		return result;
	}

	Collection<D> resolveRequires(Player player) {
		Collection<D> requires = player.getRequires();
		if (requires == null || requires.isEmpty()) {
			return requires;
		}
		Collection<D> resolved = new ArrayList<D>();
		for (D d : requires) {
			boolean tryIntercept = true;
			while (tryIntercept) {
				tryIntercept = false;
				if (dependencyInterceptors.containsKey(d)) {
					Player<D> interceptor = dependencyInterceptors.get(d);
					if (interceptor == player) {
						// keep
					} else {
						d = interceptor.getProvides().iterator().next();
						tryIntercept = true;
					}
				}
			}
			resolved.add(d);
		}
		return resolved;
	}

	protected boolean hasHigherPriority(Player<D> player1, Player<D> player2) {
		return player1.getPriority() > player2.getPriority();
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
				&& (player.resolveRequires() == null || reachedStates
						.containsAll(player.resolveRequires()));
	}

	private boolean satisfiesSomeSoughtDependenciesOrIsNotASatisfier(
			Player<D> player, Collection<D> providerDependencies) {
		return player.getProvides() == null
				|| providerDependencies != null
				&& !CommonUtils.intersection(player.getProvides(),
						providerDependencies).isEmpty();
	}

	private boolean isActive(Player<D> player) {
		return player.getPreconditions() == null
				|| reachedStates.containsAll(player.getPreconditions());
	}

	public boolean containsTask(Class<?> clazz) {
		return CollectionFilters.contains(players, new IsClassFilter(clazz));
	}

	public Set<D> getReachedStates() {
		return this.reachedStates;
	}

	Map<D, Player<D>> dependencyInterceptors = new LinkedHashMap<D, Player<D>>();

	public Collection<D> insertDependency(Collection<D> states, Player<D> player) {
		addPlayer(player);
		assert states.size() == 1;
		assert player.getProvides().size() == 1;
		dependencyInterceptors.put(states.iterator().next(), player);
		return player.getProvides();
	}

	public boolean isTrace() {
		return this.trace;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}
}
