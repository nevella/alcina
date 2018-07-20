/*
 * Copyright 2010 Google Inc.
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
package cc.alcina.framework.gwt.client.cell;

import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;

public class ShowMorePager extends AbstractPager {
	/**
	 * The default increment size.
	 */
	private static final int DEFAULT_INCREMENT = 100;

	/**
	 * The increment size.
	 */
	private int incrementSize = DEFAULT_INCREMENT;

	/**
	 * The last scroll position.
	 */
	private int lastScrollPos = 0;

	/**
	 * Construct a new {@link ShowMorePager}.
	 */
	public ShowMorePager() {
		// initWidget(scrollable);
	}

	public void attachTo(HasRows display, ScrollPanel scrollable) {
		setDisplay(display);
		// Handle scroll events.
		scrollable.addScrollHandler(new ScrollHandler() {
			@Override
			public void onScroll(ScrollEvent event) {
				// If scrolling up, ignore the event.
				int oldScrollPos = lastScrollPos;
				lastScrollPos = scrollable.getVerticalScrollPosition();
				if (oldScrollPos >= lastScrollPos) {
					return;
				}
				HasRows display = getDisplay();
				if (display == null) {
					return;
				}
				int scrollPanelContentsHeight = scrollable.getWidget()
						.getOffsetHeight();
				int scrollPanelHeight = scrollable.getOffsetHeight();
				int maxScrollTop = scrollPanelContentsHeight - scrollPanelHeight
						- 20;
				if (Math.abs(lastScrollPos - oldScrollPos) > 200) {
					// handle autoscroll to end
					return;
				}
				if (lastScrollPos >= maxScrollTop) {
					// We are near the end, so increase the page size.
					int newPageSize = Math
							.min(display.getVisibleRange().getLength()
									+ incrementSize, display.getRowCount());
					if (newPageSize != 0) {
						Range newRange = new Range(0, newPageSize);
						if (display.getVisibleRange().getStart() == newRange
								.getStart()
								&& display.getVisibleRange()
										.getLength() >= newRange.getLength()) {
							// don't show a smaller visible range (which would
							// force a search)
							return;
						}
						display.setVisibleRange(0, newPageSize);
					}
				}
			}
		});
	}

	/**
	 * Get the number of rows by which the range is increased when the scrollbar
	 * reaches the bottom.
	 *
	 * @return the increment size
	 */
	public int getIncrementSize() {
		return incrementSize;
	}

	/**
	 * Set the number of rows by which the range is increased when the scrollbar
	 * reaches the bottom.
	 *
	 * @param incrementSize
	 *            the incremental number of rows
	 */
	public void setIncrementSize(int incrementSize) {
		this.incrementSize = incrementSize;
	}

	@Override
	protected void onRangeOrRowCountChanged() {
	}
}