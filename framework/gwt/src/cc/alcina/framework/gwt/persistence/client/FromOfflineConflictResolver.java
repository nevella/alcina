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

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.widget.BlockLink;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;

@SuppressWarnings("unchecked")
/**
 * TODO - integrate with main conflict resolution framework
 * 
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint=FromOfflineConflictResolver.class,implementationType=ImplementationType.INSTANCE)
@ClientInstantiable
public class FromOfflineConflictResolver {
	@SuppressWarnings("unused")
	private Throwable caught;

	private GlassDialogBox dialog;

	private List<DeltaApplicationRecord> uncommitted;

	private LocalTransformPersistence localTransformPersistence;

	private AsyncCallback<Void> completionCallback;

	public void resolve(List<DeltaApplicationRecord> uncommitted,
			Throwable caught,
			LocalTransformPersistence localTransformPersistence,
			AsyncCallback<Void> completionCallback) {
		this.uncommitted = uncommitted;
		this.caught = caught;
		this.localTransformPersistence = localTransformPersistence;
		this.completionCallback = completionCallback;
		dialog = new GlassDialogBox();
		String title = getText(TextItem.TITLE);
		dialog.setText(title);
		dialog.add(new ResolutionOptions());
		dialog.center();
		dialog.show();
	}

	public void notResolved() {
		Window.alert(getText(TextItem.OFFLINE_NO_DISCARD_WARNING));
		Window.Location.reload();
	}

	private String getText(TextItem key) {
		return TextProvider.get().getUiObjectText(
				FromOfflineConflictResolver.class, key.toString(),
				key.getText());
	}

	enum TextItem {
		TITLE {
			@Override
			public String getText() {
				return "Problems saving offline work";
			}
		},
		OFFLINE_UPLOAD_FAILED {
			@Override
			public String getText() {
				return "<p>Upload of offline changes failed<p>\n "
						+ "Please press 'exit' to retry upload of the offline work.\n"
						+ "<hr>";
			}
		},
		OFFLINE_UPLOAD_SUCCEEDED4 {
			@Override
			public String getText() {
				return "<p>Merge of offline changes failed, but your changes were uploaded and can be merged "
						+ "by an administrator.</p><p>Your changes will be available once merged.</p>\n "
						+ "<p>Please copy the following text into an"
						+ " email and provide to an administrator:</p>"
						+ "<blockquote><b>%s</b></blockquote>"
						+ "Once you have done this, select 'discard changes'.<br>\n"
						+ "<hr>";
			}
		},
		OFFLINE_DISCARD_CHANGES {
			@Override
			public String getText() {
				return "Discard changes";
			}
		},
		OFFLINE {
			@Override
			public String getText() {
				return "Offline";
			}
		},
		OFFLINE_EXIT_NO_DISCARD {
			@Override
			public String getText() {
				return "Exit without discarding changes";
			}
		},
		OFFLINE_NO_DISCARD_WARNING {
			@Override
			public String getText() {
				return "Exiting without saving changes.\n\n"
						+ "You must successfully upload the changes,"
						+ " and press 'discard', to continue using the application.";
			}
		},
		OFFLINE_DISCARD_CONFIRMATION {
			@Override
			public String getText() {
				return "Are you sure you want to discard your changes?";
			}
		},
		OFFLINE_DISCARD_PERFORMED {
			@Override
			public String getText() {
				return "Local cache cleared";
			}
		};
		public abstract String getText();
	}

	private class ResolutionOptions extends Composite implements ClickHandler {
		private FlowPanel fp;

		private BlockLink discardLink;

		private BlockLink exitLink;

		public ResolutionOptions() {
			this.fp = new FlowPanel();
			discardLink = new BlockLink(
					getText(TextItem.OFFLINE_DISCARD_CHANGES), this);
			exitLink = new BlockLink(getText(TextItem.OFFLINE_EXIT_NO_DISCARD),
					this);
			discardLink.removeStyleName("gwt-Hyperlink");
			exitLink.removeStyleName("gwt-Hyperlink");
			boolean uploadSucceeded = LooseContext
					.getContext()
					.getBoolean(
							LocalTransformPersistence.CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED);
			String uploadFailedText = getText(TextItem.OFFLINE_UPLOAD_FAILED);
			String uploadSucceededText = getText(TextItem.OFFLINE_UPLOAD_SUCCEEDED4);
			if (uploadSucceeded) {
				uploadSucceededText = CommonUtils
						.formatJ(
								uploadSucceededText,
								"clientinstance_ids: "
										+ LooseContext
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
			} else {
				p.add(exitLink);
			}
			initWidget(fp);
		}

		public void showLog() {
			FlowPanel fp = new FlowPanel();
			TextArea ta = new TextArea();
			ta.setSize("600px", "300px");
			String text = CommonUtils.formatJ("Unsaved transforms\n\n" + "%s",
					uncommitted.toString());
			ta.setText(text);
			fp.add(ta);
			ClientNotifications cn = Registry.impl(ClientNotifications.class);
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
				Registry.impl(ClientNotifications.class).log("pre-clear-db");
				localTransformPersistence
						.clearAllPersisted(new AsyncCallback() {
							@Override
							public void onSuccess(Object result) {
								Window.alert(getText(TextItem.OFFLINE_DISCARD_PERFORMED));
								dialog.hide();
								completionCallback.onSuccess(null);
								Registry.impl(ClientNotifications.class).log(
										"post-clear-db");
							}

							@Override
							public void onFailure(Throwable caught) {
								Window.alert(caught.getMessage());
								Window.Location.reload();
							}
						});
			}
			if (sender == exitLink) {
				dialog.hide();
				notResolved();
			}
		}
	}
}
