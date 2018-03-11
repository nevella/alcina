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

import static com.google.gwt.dom.client.BrowserEvents.*;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.builder.shared.HtmlAnchorBuilder;
import com.google.gwt.dom.builder.shared.HtmlBuilderFactory;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

/**
 * A cell that renders a button and takes a delegate to perform actions on
 * mouseUp.
 *
 * @param <C>
 *            the type that this Cell represents
 */
public class PlaceLinkCell extends AbstractCell<TextPlaceTuple> {
	/**
	 * Construct a new {@link PlaceLinkCell}.
	 *
	 * @param message
	 *            the message to display on the button
	 * @param delegate
	 *            the delegate that will handle events
	 */
	public PlaceLinkCell() {
		super(CLICK, KEYDOWN);
	}

	@Override
	public void onBrowserEvent(Context context, Element parent,
			TextPlaceTuple value, NativeEvent event,
			ValueUpdater<TextPlaceTuple> valueUpdater) {
		super.onBrowserEvent(context, parent, value, event, valueUpdater);
		if (CLICK.equals(event.getType())) {
			EventTarget eventTarget = event.getEventTarget();
			if (!Element.is(eventTarget)) {
				return;
			}
			if (parent.getFirstChildElement()
					.isOrHasChild(Element.as(eventTarget))) {
				event.stopPropagation();
				// // Ignore clicks that occur outside of the main element.
				// onEnterKeyDown(context, parent, value, event, valueUpdater);
			}
		}
	}

	@Override
	public void render(Context context, TextPlaceTuple value,
			SafeHtmlBuilder sb) {
		if (value.place == null) {
			sb.append(SafeHtmlUtils.fromTrustedString("No link"));
		}
		HtmlBuilderFactory factory = HtmlBuilderFactory.get();
		HtmlAnchorBuilder builder = factory.createAnchorBuilder();
		builder.href("#" + RegistryHistoryMapper.get().getToken(value.place));
		builder.text(value.text);
		sb.append(builder.asSafeHtml());
	}

	@Override
	protected void onEnterKeyDown(Context context, Element parent,
			TextPlaceTuple value, NativeEvent event,
			ValueUpdater<TextPlaceTuple> valueUpdater) {
		// delegate.execute(value);
	}
}
