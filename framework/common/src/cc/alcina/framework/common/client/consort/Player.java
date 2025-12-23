package cc.alcina.framework.common.client.consort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface Player<D> {
	static class Support<D> {
		Player<D> player;

		public Runnable runnable;

		Consort<D> consort;

		boolean asynchronous;

		List<D> requires = new ArrayList<D>();

		List<D> provides = new ArrayList<D>();

		long start;

		Support(Player<D> player) {
			this.player = player;
		}
	}

	static int PRIORITY_LOW = 1;

	static int PRIORITY_NORMAL = 100;

	static int PRIORITY_IMMEDIATE = 1000;

	Player.Support<D> support();

	default void addProvides(D state) {
		support().provides.add(state);
	}

	default void addRequires(D state) {
		support().requires.add(state);
	}

	default void cancel() {
	}

	default boolean canRunInParallelWith(Player<D> otherPlayer) {
		return false;
	}

	default Logger getLogger() {
		return getConsort().logger;
	}

	default Collection<D> getPreconditions() {
		return Collections.emptyList();
	}

	default int getPriority() {
		return Player.PRIORITY_NORMAL;
	}

	/**
	 * Important - unenforced is the idea that, for a non-parallel machine, only
	 * one state will be provided per player - still working on this, but see
	 * Consort.satisfiesSomeSoughtDependenciesOrIsNotASatisfier
	 */
	default Collection<D> getProvides() {
		return support().provides;
	}

	default Collection<D> getRequires() {
		return support().requires;
	}

	default long getStart() {
		return support().start;
	}

	default boolean isAllowEqualPriority() {
		return false;
	}

	default boolean isAsynchronous() {
		return support().asynchronous;
	}

	default boolean isCancellable() {
		return true;
	}

	default boolean isPerConsortSingleton() {
		return false;
	}

	default boolean isRemoveAfterPlay() {
		return getProvides().isEmpty();
	}

	default void logToInfo(String string, Object... args) {
		getConsort().logger.info(string, args);
	}

	default void onFailure(Throwable caught) {
		getConsort().onFailure(caught);
	}

	default void play(boolean replaying) {
		if (replaying) {
			((LoopingPlayer) this).loop();
		} else {
			support().start = System.currentTimeMillis();
			support().runnable.run();
		}
		if (!isAsynchronous() && getProvides().size() <= 1) {
			wasPlayed();
		}
	}

	default String provideNameForTransitions() {
		return getClass().getSimpleName();
	}

	default void removeRequires(D... requiresStates) {
		support().requires.removeAll(Arrays.asList(requiresStates));
	}

	default void setAsynchronous(boolean asynchronous) {
		support().asynchronous = asynchronous;
	}

	default void setConsort(Consort<D> consort) {
		support().consort = consort;
	}

	default String shortName() {
		return CommonUtils.simpleClassName(getClass());
	}

	default void wasPlayed() {
		getConsort().wasPlayed(this);
	}

	default Consort<D> getConsort() {
		return support().consort;
	}

	default void wasPlayed(D dep) {
		getConsort().wasPlayed(this, dep == null ? Collections.EMPTY_LIST
				: Collections.singletonList(dep));
	}
}
