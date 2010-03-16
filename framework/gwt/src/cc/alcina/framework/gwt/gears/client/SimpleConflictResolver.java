/* 
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

package cc.alcina.framework.gwt.gears.client;

import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.widget.BlockLink;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */

 public class SimpleConflictResolver {
	private Throwable caught;

	private GlassDialogBox dialog;

	private List<DTRSimpleSerialWrapper> uncommitted;

	private SimpleGearsTransformPersistence simpleGearsTransformPersistence;

	private Callback completionCallback;

	public void resolve(List<DTRSimpleSerialWrapper> uncommitted,
			Throwable caught,
			SimpleGearsTransformPersistence simpleGearsTransformPersistence,
			Callback cb) {
		this.uncommitted = uncommitted;
		this.caught = caught;
		this.simpleGearsTransformPersistence = simpleGearsTransformPersistence;
		this.completionCallback = cb;
		dialog = new GlassDialogBox();
		dialog.setText("Conflicts saving offline work");
		dialog.add(new ResolutionOptions());
		dialog.center();
		dialog.show();
	}

	public void notResolved() {
		throw new WrappedRuntimeException("Unable to save prior work. "
				+ "You'll be unable to use " + "this application"
				+ " until saving prior work succeeds", caught,
				SuggestedAction.NOTIFY_WARNING);
	}

	private class ResolutionOptions extends Composite implements ClickHandler {
		private BlockLink displayLink;

		private FlowPanel fp;

		private BlockLink discardLink;

		private BlockLink exitLink;

		public ResolutionOptions() {
			this.fp = new FlowPanel();
			displayLink = new BlockLink(TextProvider.get().getUiObjectText(
					SimpleConflictResolver.class, "display-changes-link",
					"Display changes"), this);
			displayLink.removeStyleName("gwt-Hyperlink");
			discardLink = new BlockLink(TextProvider.get().getUiObjectText(
					SimpleConflictResolver.class, "discard-changes-link",
					"Discard changes"), this);
			exitLink = new BlockLink(TextProvider.get().getUiObjectText(
					SimpleConflictResolver.class, "exit-link", "Exit"), this);
			displayLink.removeStyleName("gwt-Hyperlink");
			discardLink.removeStyleName("gwt-Hyperlink");
			exitLink.removeStyleName("gwt-Hyperlink");
			HTML html = new HTML(
					TextProvider
							.get()
							.getUiObjectText(
									SimpleConflictResolver.class,
									"resolution-procedure-text-1",
									"<p>Some conflicts or other problems "
											+ "with saving your changes need resolution</p>"
											+ "<p><b>Option 1</b></p><div class='pad-left-15'><p>First, click 'display changes', "
											+ "then open an email, press 'paste', and "
											+ "send it to your system administrator.</p>"
											+ "<p>Then click 'discard changes', to clear your "
											+ "computer's copy of the changes, and let"
											+ " you connect"
											+ " to the application.</p>"
											+ "<p>Your changes will need to be adjusted by "
											+ "the administrator, so you may not see them"
											+ " for a few days.</p></div>"
											+ "<p><b>Option 2</b></p><div class='pad-left-15'><p>Click 'discard changes'"
											+ " - your changes will be discarded, but you'll "
											+ "be able to continue working.</p></div><hr>"));
			fp.add(html);
			FlowPanel p = new FlowPanel();
			p.setStyleName("pad-15");
			fp.add(p);
			p.add(displayLink);
			p.add(discardLink);
			p.add(exitLink);
			initWidget(fp);
		}

		public void showLog() {
			FlowPanel fp = new FlowPanel();
			TextArea ta = new TextArea();
			ta.setSize("600px", "300px");
			String text = CommonUtils.format("Unsaved transforms\n\n" + "%1",
					uncommitted.toString());
			ta.setText(text);
			fp.add(ta);
			ClientBase clientBase = ClientLayerLocator.get().clientBase();
			clientBase.setDialogAnimationEnabled(false);
			clientBase.showMessage(fp);
			clientBase.setDialogAnimationEnabled(true);
			ta.setSelectionRange(0, text.length());
			copy();
		}


		protected native void copy() /*-{
			$doc.execCommand("Copy");
		}-*/;

		@SuppressWarnings("unchecked")
		public void onClick(ClickEvent event) {
			Widget sender = (Widget) event.getSource();
			if (sender == displayLink) {
				showLog();
			}
			if (sender == discardLink) {
				if (Window.confirm(TextProvider.get().getUiObjectText(
						SimpleConflictResolver.class, "discard-confirmation",
						"Are you sure you want to discard your changes?"))) {
					simpleGearsTransformPersistence.clearPersisted();
					Window.alert(TextProvider.get().getUiObjectText(
							SimpleConflictResolver.class, "discard-complete",
							"Changes discarded"));
					dialog.hide();
					completionCallback.callback(null);
				}
			}
			if (sender == exitLink) {
				dialog.hide();
				notResolved();
			}
		}
	}
}
