package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.mutations.ElementSelectionRangeRecord;
import com.google.gwt.dom.client.mutations.SelectionRecord;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * <p>
 * An instance of this class is sent from the browser to the romcom server to
 * try and optimise (remove the need for most) queries from the server to the
 * dom.
 * 
 * <p>
 * Instances are sent when client DOM events occur, or when the DOM is changed.
 * 
 * <p>
 * It contains the non-dom browser objects most commonly needed by handling code
 * - focus, scroll position, selection etc
 * 
 * <p>
 * Data are sent for: [window, all ancestors inclusive of the active element,
 * elements with attribute -rc-transmit-state]
 */
@Bean(PropertySource.FIELDS)
public final class WindowState {
	/**
	 * The dimensions and scroll pos of a Node (Document corresonds to Window)
	 */
	@Bean(PropertySource.FIELDS)
	public final static class NodeUiState {
		public DomRect boundingClientRect;

		public IntPair scrollPos;

		public AttachId element;
	}

	public AttachId activeElement;

	public List<NodeUiState> nodeUiStates = new ArrayList<>();

	transient Map<Integer, NodeUiState> idUiState;

	public int clientHeight;

	public int clientWidth;

	public int scrollTop;

	public int scrollLeft;

	public NodeUiState uiStateFor(int attachId) {
		if (idUiState == null) {
			idUiState = nodeUiStates.stream().collect(
					AlcinaCollectors.toKeyMap(state -> state.element.id));
		}
		return idUiState.get(attachId);
	}
}