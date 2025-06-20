/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dom.client;

import java.util.List;
import java.util.Objects;

import com.google.gwt.dom.client.mutations.SelectionRecord;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.DomLocation;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Models the browser selection object. Normally use the exposed Location
 * objects, since they handle DOM's "unique" addressing, and track mutations
 *
 */
public class Selection implements ClientDomSelection {
	SelectionLocal local;

	private ClientDomSelection remote = null;

	protected Selection(Document document) {
		local = new SelectionLocal(this);
		ClientDomDocument remoteDocument = document.remote();
		if (remoteDocument != null) {
			remote = remoteDocument.ensureRemoteSelection(this);
		}
	}

	protected SelectionLocal local() {
		return local;
	}

	/**
	 * The StyleJso remote is only instantiated lazily (it's mostly unused)
	 * 
	 * @return the remote if it exists (essentially if the element has a
	 *         remote), or null
	 */
	protected ClientDomSelection remote() {
		return remote;
	}

	@Override
	public Selection selectionObject() {
		return this;
	}

	@Override
	public void collapse(Node node) {
		local.collapse(node);
		remote.collapse(node);
	}

	@Override
	public void collapse(Node node, int offset) {
		local.collapse(node, offset);
		remote.collapse(node, offset);
	}

	@Override
	public void extend(Node node) {
		local.extend(node);
		remote.extend(node);
	}

	@Override
	public void extend(Node node, int offset) {
		local.extend(node, offset);
		remote.extend(node, offset);
	}

	/**
	 * Note - this is not the "anchor node", if it's an element, rather its
	 * parent. In general, use getAnchorLocation (also, locations track
	 * mutations)
	 */
	@Override
	public Node getAnchorNode() {
		return local.getAnchorNode();
	}

	@Override
	public int getAnchorOffset() {
		return local.getAnchorOffset();
	}

	@Override
	public DomRect getClientRect() {
		return local.getClientRect();
	}

	/**
	 * Note - this is not the "focussed node", if it's an element, rather its
	 * parent. In general, use getFocusLocation
	 */
	@Override
	public Node getFocusNode() {
		return local.getFocusNode();
	}

	@Override
	public int getFocusOffset() {
		return local.getFocusOffset();
	}

	@Override
	public String getType() {
		return local.getType();
	}

	@Override
	public boolean isCollapsed() {
		return local.isCollapsed();
	}

	@Override
	public void modify(String alter, String direction, String granularity) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'modify'");
	}

	@Override
	public void removeAllRanges() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'removeAllRanges'");
	}

	@Override
	public SelectionRecord getSelectionRecord() {
		return local.getSelectionRecord();
	}

	void onDocumentEventSystemInit() {
		local.onDocumentEventSystemInit();
	}

	public boolean hasSelection() {
		return !Objects.equals("None", getType());
	}

	public void validate() {
		local.validate();
	}

	public Location getAnchorLocation() {
		return cachedLocations.ensureCurrent().anchorLocation;
	}

	public Location getFocusLocation() {
		return cachedLocations.ensureCurrent().focusLocation;
	}

	CachedLocations cachedLocations = new CachedLocations();

	class CachedLocations {
		Location anchorLocation;

		Location focusLocation;

		SelectionRecord lastSelectionRecord;

		CachedLocations ensureCurrent() {
			SelectionRecord currenSelectionRecord = local()
					.getSelectionRecord();
			if (currenSelectionRecord != lastSelectionRecord) {
				if (currenSelectionRecord == null) {
					anchorLocation = null;
					focusLocation = null;
				} else {
					anchorLocation = asLocation(getAnchorNode(),
							getAnchorOffset());
					focusLocation = asLocation(getFocusNode(),
							getFocusOffset());
				}
				lastSelectionRecord = currenSelectionRecord;
			}
			return this;
		}

		Location asLocation(Node node, int offset) {
			if (node == null) {
				return null;
			}
			DomNode domNode = node.asDomNode();
			if (domNode.isText()) {
				return domNode.asLocation().textRelativeLocation(offset, false);
			} else {
				// note that - for node.class == Element - offset can be after
				// all
				// children
				if (offset == node.getChildCount()) {
					return domNode.asRange().end;
				} else {
					return node.getChild(offset).asDomNode().asLocation();
				}
			}
		}
	}

	public Location.Range asRange() {
		return new Location.Range(getAnchorLocation(), getFocusLocation());
	}

	@Override
	public String toString() {
		return local.toString();
	}

	public void select(Node node) {
		collapse(node);
		extend(node, node.getChildCount());
	}

	public void extend(Location toLocation) {
		DomLocation domLocation = toLocation.asDomLocation();
		extend((Node) domLocation.getNode(), domLocation.getOffset());
	}

	public void collapse(Location toLocation) {
		DomLocation domLocation = toLocation.asDomLocation();
		collapse((Node) domLocation.getNode(), domLocation.getOffset());
	}

	public void deleteFromDocument() {
		/*
		 * prepare the post-delete cursor, delete the range, position the cursor
		 */
		List<Location.Range> treeRanges = asRange().asTreeRanges();
		Location focusLocation = getFocusLocation();
		Location postDeleteCursorPosition = null;
		if (focusLocation.isTextNode()) {
			Location.Range focusRange = treeRanges.stream().filter(tr -> tr
					.containingNode() == focusLocation.getContainingNode())
					.findFirst().orElse(null);
			/*
			 * TODO - just let the dom do this for now
			 */
		}
		asRange().delete();
	}
}
