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
package cc.alcina.framework.gwt.persistence.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContextProvider;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientNotifications;
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
 * TODO - integrate with main conflict resolution framework
 * 
 * @author Nick Reddel
 */
public class FromOfflineConflictResolver {
	@SuppressWarnings("unused")
	private Throwable caught;

	private GlassDialogBox dialog;

	private List<DTRSimpleSerialWrapper> uncommitted;

	private LocalTransformPersistence localTransformPersistence;

	private Callback completionCallback;

	public void resolve(List<DTRSimpleSerialWrapper> uncommitted,
			Throwable caught,
			LocalTransformPersistence localTransformPersistence, Callback cb) {
		this.uncommitted = uncommitted;
		this.caught = caught;
		this.localTransformPersistence = localTransformPersistence;
		this.completionCallback = cb;
		dialog = new GlassDialogBox();
		String title = TextProvider.get().getUiObjectText(
				FromOfflineConflictResolver.class, "title",
				"Problems saving offline work");
		dialog.setText(title);
		dialog.add(new ResolutionOptions());
		dialog.center();
		dialog.show();
	}

	public void notResolved() {
		Window.alert(localisedMessages.get(TextItem.OFFLINE_NO_DISCARD_WARNING));
		Window.Location.reload();
	}

	Map<TextItem, String> localisedMessages = new HashMap<FromOfflineConflictResolver.TextItem, String>();

	public void initLocalisedTexts() {
		localisedMessages
				.put(TextItem.OFFLINE_UPLOAD_FAILED,
						TextProvider
								.get()
								.getUiObjectText(
										FromOfflineConflictResolver.class,
										TextItem.OFFLINE_UPLOAD_FAILED
												.toString(),
										"<p>Upload of offline changes failed<p>\n "
												+ "Please press 'exit' to retry upload of the offline work.\n"
												+ "<hr>"));
		localisedMessages
				.put(TextItem.OFFLINE_UPLOAD_SUCCEEDED4,
						TextProvider
								.get()
								.getUiObjectText(
										FromOfflineConflictResolver.class,
										TextItem.OFFLINE_UPLOAD_SUCCEEDED4
												.toString(),
										"<p>Merge of offline changes failed, but your changes were uploaded and can be merged "
												+ "by an administrator.</p><p>Your changes will be available once merged.</p>\n "
												+ "<p>Please copy the following text into an email and provide to an administrator:</p>"
												+ "<blockquote><b>%s</b></blockquote>"
												+ "Once you have done this, select 'discard changes'.<br>\n"
												+ "<hr>"));
		localisedMessages.put(
				TextItem.OFFLINE_DISCARD_CHANGES,
				TextProvider.get().getUiObjectText(
						FromOfflineConflictResolver.class,
						TextItem.OFFLINE_DISCARD_CHANGES.toString(),
						"Discard changes"));
		localisedMessages.put(
				TextItem.OFFLINE_EXIT_NO_DISCARD,
				TextProvider.get().getUiObjectText(
						FromOfflineConflictResolver.class,
						TextItem.OFFLINE_EXIT_NO_DISCARD.toString(),
						"Exit without discarding changes"));
		localisedMessages
				.put(TextItem.OFFLINE_NO_DISCARD_WARNING,
						TextProvider
								.get()
								.getUiObjectText(
										FromOfflineConflictResolver.class,
										TextItem.OFFLINE_NO_DISCARD_WARNING
												.toString(),
										"Exiting without saving changes.\n\n"
												+ "You must successfully upload the changes, and press 'discard', to continue using the application."));
	}

	enum TextItem {
		OFFLINE_UPLOAD_FAILED, OFFLINE_UPLOAD_SUCCEEDED4,
		OFFLINE_DISCARD_CHANGES, OFFLINE, OFFLINE_EXIT_NO_DISCARD,
		OFFLINE_NO_DISCARD_WARNING
	}

	private class ResolutionOptions extends Composite implements ClickHandler {
		private FlowPanel fp;

		private BlockLink discardLink;

		private BlockLink exitLink;

		public ResolutionOptions() {
			initLocalisedTexts();
			this.fp = new FlowPanel();
			discardLink = new BlockLink(
					localisedMessages.get(TextItem.OFFLINE_DISCARD_CHANGES),
					this);
			exitLink = new BlockLink(
					localisedMessages.get(TextItem.OFFLINE_EXIT_NO_DISCARD),
					this);
			discardLink.removeStyleName("gwt-Hyperlink");
			exitLink.removeStyleName("gwt-Hyperlink");
			boolean uploadSucceeded = LooseContextProvider
					.getContext()
					.getBoolean(
							LocalTransformPersistence.CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED);
			String uploadFailedText = localisedMessages
					.get(TextItem.OFFLINE_UPLOAD_FAILED);
			String uploadSucceededText = localisedMessages
					.get(TextItem.OFFLINE_UPLOAD_SUCCEEDED4);
			if (uploadSucceeded) {
				uploadSucceededText = CommonUtils
						.formatJ(
								uploadSucceededText,
								"clientinstance_ids: "
										+ LooseContextProvider
												.getContext()
												.get(LocalTransformPersistence.CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED_CLIENT_IDS));
			}
			HTML html = new HTML(uploadSucceeded ? uploadSucceededText
					: uploadFailedText);
			fp.add(html);
			FlowPanel p = new FlowPanel();
			p.setStyleName("pad-15");
			fp.add(p);
			if (uploadSucceeded) {
				p.add(discardLink);
			}
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
			ClientNotifications cn = ClientLayerLocator.get().notifications();
			cn.setDialogAnimationEnabled(false);
			cn.showMessage(fp);
			cn.setDialogAnimationEnabled(true);
			// ta.setSelectionRange(0, text.length());
			// copy();
			// no browser permits this
		}

		protected native void copy() /*-{
			$doc.execCommand("Copy");
		}-*/;

		@SuppressWarnings("unchecked")
		public void onClick(ClickEvent event) {
			Widget sender = (Widget) event.getSource();
			if (sender == discardLink) {
				if (Window.confirm(TextProvider.get().getUiObjectText(
						FromOfflineConflictResolver.class,
						"discard-confirmation",
						"Are you sure you want to discard your changes?"))) {
					ClientLayerLocator.get().notifications()
							.log("pre-clear-db");
					localTransformPersistence
							.clearAllPersisted(new PersistenceCallback() {
								@Override
								public void onSuccess(Object result) {
									Window.alert(TextProvider
											.get()
											.getUiObjectText(
													FromOfflineConflictResolver.class,
													"discard-complete",
													"Changes discarded"));
									dialog.hide();
									completionCallback.callback(null);
									ClientLayerLocator.get().notifications()
											.log("post-clear-db");
								}

								@Override
								public void onFailure(Throwable caught) {
									Window.alert(caught.getMessage());
									Window.Location.reload();
								}
							});
				}
			}
			if (sender == exitLink) {
				dialog.hide();
				notResolved();
			}
		}
	}
}
