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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import cc.alcina.framework.common.client.util.Ax;

/**
 * A cell that renders a button and takes a delegate to perform actions on
 * mouseUp.
 *
 * @param <C>
 *            the type that this Cell represents
 */
public class UnsafeHtmlCell extends AbstractCell<TextTitleTuple> {
	/**
	 * Construct a new {@link UnsafeHtmlCell}.
	 *
	 * @param message
	 *            the message to display on the button
	 * @param delegate
	 *            the delegate that will handle events
	 */
	public UnsafeHtmlCell() {
	}

	@Override
	public void render(Context context, TextTitleTuple value,
			SafeHtmlBuilder sb) {
		String prelude = "<div>";
		if (Ax.notBlank(value.title)) {
			prelude = Ax.format("<div title='%s'>",
					SafeHtmlUtils.htmlEscape(value.title));
		}
		sb.append(SafeHtmlUtils.fromTrustedString(prelude));
		sb.append(SafeHtmlUtils.fromTrustedString(Ax.blankToEmpty(value.text)));
		sb.append(SafeHtmlUtils.fromTrustedString("</div>"));
	}
}
