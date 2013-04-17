package cc.alcina.framework.common.client.state;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
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
public class Zone {
	public static final transient String BEFORE_PLAY = "BEFORE_PLAY";

	public static final transient String AFTER_PLAY = "AFTER_PLAY";

	private TopicPublisher topicPublisher = new TopicPublisher();

	LinkedList<Player> players = new LinkedList<Player>();

	public void addPlayer(Player player) {
		player.setZone(this);
		players.addLast(player);
		consumeQueue();
	}

	private boolean executingPlayer = false;

	private boolean consumingQueue;

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

	public void wasPlayed(Player player) {
		executingPlayer = false;
		topicPublisher.publishTopic(AFTER_PLAY, player);
		consumeQueue();
	}

	public void nudge() {
		consumeQueue();
	}

	protected void consumeQueue() {
		if (executingPlayer || consumingQueue) {
			return;
		}
		consumingQueue = true;
		// this means that synchronous players will be dispatched sequentially
		// within the while loop, but async tasks will be dispatched by
		// (non-recursive) consumeQueue/wasPlayed calls
		while (!executingPlayer) {
			Player player = nextPlayer();
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

	protected void executePlayer(Player player) {
		topicPublisher.publishTopic(BEFORE_PLAY, player);
		player.play();
	}

	private void maybeRemovePlayersFromQueue(Player player) {
		if (player.isPerZoneSingleton()) {
			CollectionFilters.filterInPlace(players, new InverseFilter(
					new IsClassFilter(player.getClass())));
		} else {
			players.remove(player);
		}
		if (!player.isRemoveAfterPlay()) {
			players.add(player);
		}
	}

	private Set<Object> reachedStates = new LinkedHashSet<Object>();

	private Player nextPlayer() {
		Player result = null;
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
		Set<Object> providerDependencies = new LinkedHashSet<Object>();
		while (true) {
			Set<Object> seenDependencies = new LinkedHashSet<Object>();
			for (Player player : players) {
				if (isActive(player)) {
					if (satisfiesDeps(player, providerDependencies)) {
						if (result == null || hasHigherPriority(player, result)) {
							result = player;
						}
					} else {
						if (satisfiesSomeSoughtDependenciesOrIsNotASatisfier(
								player, providerDependencies)) {
							seenDependencies.addAll(player.getDependencies());
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

	protected boolean hasHigherPriority(Player player1, Player player2) {
		return player1.getPriority() > player2.getPriority();
	}

	/*
	 * In first pass, just go for immediately satisfied, non-state-provider
	 * players
	 * 
	 * In the second, we look for state-providers in an expanding loop, (for
	 * dependency chains), looking for any path forward
	 */
	private boolean satisfiesDeps(Player player, Collection providerDependencies) {
		return satisfiesSomeSoughtDependenciesOrIsNotASatisfier(player,
				providerDependencies)
				&& (player.getDependencies() == null || reachedStates
						.containsAll(player.getDependencies()));
	}

	private boolean satisfiesSomeSoughtDependenciesOrIsNotASatisfier(
			Player player, Collection providerDependencies) {
		return player.getProvides() == null
				|| providerDependencies != null
				&& !CommonUtils.intersection(player.getProvides(),
						providerDependencies).isEmpty();
	}

	private boolean isActive(Player player) {
		return player.getPreconditions() == null
				|| reachedStates.containsAll(player.getPreconditions());
	}

	public boolean containsTask(Class<?> clazz) {
		return CollectionFilters.contains(players, new IsClassFilter(clazz));
	}

	public Set<Object> getReachedStates() {
		return this.reachedStates;
	}
}
