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
package cc.alcina.framework.gwt.client.widget.dialog;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CancelAction;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.widget.HasFirstFocusable;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;

/**
 * 
 * @author Nick Reddel
 */
public class OkCancelDialogBox extends GlassDialogBox {
	protected Button cancelButton;

	protected Button okButton;

	protected final Widget widget;

	protected PermissibleActionListener vetoableActionListener;

	private HorizontalPanel buttonsPanel;

	public OkCancelDialogBox(String title, Widget widget,
			final PermissibleActionListener l) {
		this(title, widget, l, HasHorizontalAlignment.ALIGN_CENTER);
	}

	protected String getOKButtonName() {
		return "OK";
	}

	protected boolean showAnimated() {
		return true;
	}

	// makes sure richtextareas get a focuslost()
	public void focusOK() {
		okButton.setFocus(true);
	}

	public OkCancelDialogBox(String title, Widget widget,
			PermissibleActionListener listener,
			HorizontalAlignmentConstant widgetAlign) {
		this.widget = widget;
		this.vetoableActionListener = listener;
		setText(title);
		setAnimationEnabled(showAnimated());
		VerticalPanel vp = new VerticalPanel();
		vp.add(widget);
		vp.setCellHorizontalAlignment(widget, widgetAlign);
		buttonsPanel = new HorizontalPanel();
		cancelButton = new Button("Cancel");
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				OkCancelDialogBox.this.hide();
				vetoableActionListener
						.vetoableAction(new PermissibleActionEvent(this,
								CancelAction.INSTANCE));
			}
		});
		okButton = new Button(getOKButtonName());
		okButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				onOkButtonClicked();
			}
		});
		buttonsPanel
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		buttonsPanel.setSpacing(8);
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		vp.add(buttonsPanel);
		vp.setCellHorizontalAlignment(buttonsPanel,
				HasHorizontalAlignment.ALIGN_CENTER);
		setWidget(vp);
		adjustDisplay();
		center();
	}

	protected void onOkButtonClicked() {
		if (!checkValid()) {
			return;
		}
		okButton.setEnabled(false);
		OkCancelDialogBox.this.hide();
		if (vetoableActionListener != null) {
			vetoableActionListener.vetoableAction(new PermissibleActionEvent(
					this, OkAction.INSTANCE));
		}
	}

	// for subclasses
	protected void adjustDisplay() {
	}

	private boolean isCentering = false;

	private Timer checkReCenterTimer;

	@Override
	public void center() {
		isCentering = true;
		super.center();
		isCentering = false;
	}

	@Override
	public void hide() {
		super.hide();
	}

	@Override
	public void show() {
		super.show();
		if (!isCentering) {
			okButton.setFocus(true);
		}
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		checkReCenterTimer = new Timer() {
			private int lastCenterHeight;

			@Override
			public void run() {
				if (lastCenterHeight != 0
						&& lastCenterHeight != getOffsetHeight()) {
					center();
				}
				lastCenterHeight = getOffsetHeight();
			}
		};
		checkReCenterTimer.scheduleRepeating(200);
	}

	@Override
	protected void onDetach() {
		checkReCenterTimer.cancel();
		super.onDetach();
	}

	protected boolean checkValid() {
		return true;
	}

	public static class SaveWithValidatorDialogBox extends OkCancelDialogBox {
		protected final Binding binding;

		public SaveWithValidatorDialogBox(String title, Widget widget,
				PermissibleActionListener l,
				HorizontalAlignmentConstant widgetAlign, Binding binding) {
			super(title, widget, l, widgetAlign);
			this.binding = binding;
		}

		@Override
		public void show() {
			super.show();
			if (widget instanceof HasFirstFocusable) {
				HasFirstFocusable ff = (HasFirstFocusable) widget;
				ff.firstFocusable().setFocus(true);
			}
		}

		@Override
		protected boolean checkValid() {
			if (binding.validate()) {
				return true;
			}
			GwittirUtils.refreshEmptyTextBoxes(binding);
			notifyProblem();
			return false;
		}

		protected void notifyProblem() {
			ClientLayerLocator.get().notifications()
					.showWarning("Please correct the problems in the form");
		}

		@Override
		protected String getOKButtonName() {
			return "Save";
		}
	}

	public HorizontalPanel getButtonsPanel() {
		return this.buttonsPanel;
	}
}
