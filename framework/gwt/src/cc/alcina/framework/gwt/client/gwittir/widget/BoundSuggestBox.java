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
package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.Objects;
import java.util.Optional;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleModel;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleSuggestion;
import cc.alcina.framework.gwt.client.logic.CancellableAsyncCallback;

/**
 * 
 * @author Nick Reddel
 */
@SuppressWarnings("deprecation")
public class BoundSuggestBox<T> extends AbstractBoundWidget<T>
		implements Focusable {
	protected SuggestBox base;

	private T value;

	
	private Renderer<T, String> renderer = (Renderer) ToStringRenderer.INSTANCE;

	private BoundSuggestOracle suggestOracle;

	private boolean withPlaceholder = true;

	private boolean showOnFocus = false;

	/** Creates a new instance of Label */
	public BoundSuggestBox() {
	}

	public String getLastFilterText() {
		return base.getText();
	}

	/**
	 * Get the value of renderer
	 * 
	 * @return the value of renderer
	 */
	public Renderer<T, String> getRenderer() {
		return this.renderer;
	}

	public T getValue() {
		return value;
	}

	public boolean isShowOnFocus() {
		return this.showOnFocus;
	}

	public boolean isWithPlaceholder() {
		return this.withPlaceholder;
	}

	public void setFilterText(String filterText) {
		base.setText(filterText);
		if (!Ax.isBlank(filterText)) {
			base.showSuggestions(filterText);
		}
	}

	@Override
	public void setModel(Object model) {
		super.setModel(model);
		suggestOracle.model = model;
	}

	public void setRenderer(Renderer<T, String> newrenderer) {
		this.renderer = newrenderer;
	}

	public void setShowOnFocus(boolean showOnFocus) {
		this.showOnFocus = showOnFocus;
	}

	
	public void setValue(T value) {
		T old = this.getValue();
		this.value = value;
		this.base.setText(renderer.apply(value));
		if (this.getValue() != old && (this.getValue() == null
				|| (this.getValue() != null && !this.getValue().equals(old)))) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	private String placeholderText = "Type for suggestions";

	public String getPlaceholderText() {
		return this.placeholderText;
	}

	public void setPlaceholderText(String placeholderText) {
		this.placeholderText = placeholderText;
	}

	public void setWithPlaceholder(boolean withPlaceholder) {
		this.withPlaceholder = withPlaceholder;
	}

	public void suggestOracle(BoundSuggestOracle suggestOracle) {
		this.suggestOracle = suggestOracle;
		base = new SuggestBox(suggestOracle);
		((DefaultSuggestionDisplay) base.getSuggestionDisplay())
				.setPopupStyleName("bound-suggest-box-popup");
		((DefaultSuggestionDisplay) base.getSuggestionDisplay())
				.setMatchTextBoxWidth(true);
		((DefaultSuggestionDisplay) base.getSuggestionDisplay())
				.setMatchTextBoxAdjust(-4);
		if (withPlaceholder) {
			base.getValueBox().getElement().setPropertyString("placeholder",
					placeholderText);
		}
		base.addSelectionHandler(evt -> {
			if (evt.getSelectedItem() != null) {
				setValue((T) ((BoundSuggestOracleSuggestion) evt
						.getSelectedItem()).typedValue);
			}
		});
		base.addFocusListener(new FocusListener() {
			@Override
			public void onFocus(Widget sender) {
				TextBoxBase baseBox = (TextBoxBase) base.getValueBox();
				String baseTextAtFocusTime = base.getText();
				if (showOnFocus || !Ax.isBlank(baseTextAtFocusTime)) {
					if (!base.isInSuggestionCallback()) {
						if (!Ax.isBlank(baseTextAtFocusTime)) {
							Scheduler.get().scheduleDeferred(() -> {
								if (baseBox.getText()
										.equals(baseTextAtFocusTime)) {
									baseBox.setSelectionRange(0,
											baseBox.getText().length());
								}
							});
						}
						base.showSuggestions(baseTextAtFocusTime);
					}
				}
			}

			@Override
			public void onLostFocus(Widget sender) {
				if (!Objects.equals(base.getText(), renderer.apply(value))) {
					setValue(null);
				}
			}
		});
		super.initWidget(base);
	}

	public static class BoundSuggestOracle extends SuggestOracle {
		public Object model;

		private Class clazz;

		private String hint;

		protected CancellableAsyncCallback runningCallback = null;

		public BoundSuggestOracle() {
		}

		public BoundSuggestOracle clazz(Class clazz) {
			this.clazz = clazz;
			return this;
		}

		public BoundSuggestOracle hint(String hint) {
			this.hint = hint;
			return this;
		}

		@Override
		public void requestDefaultSuggestions(Request request,
				Callback callback) {
			requestSuggestions(request, callback);
		}

		@Override
		public void requestSuggestions(Request request, Callback callback) {
			BoundSuggestOracleRequest boundRequest = new BoundSuggestOracleRequest();
			boundRequest.setLimit(request.getLimit());
			boundRequest.setQuery(request.getQuery());
			boundRequest.targetClassName = clazz.getName();
			boundRequest.hint = hint;
			if (model instanceof BoundSuggestOracleModel) {
				boundRequest.model = (BoundSuggestOracleModel) model;
			}
			Optional.ofNullable(runningCallback)
					.ifPresent(sc -> sc.setCancelled(true));
			runningCallback = new SuggestCallback(request, callback);
			ClientBase.getCommonRemoteServiceAsyncInstance()
					.suggest(boundRequest, runningCallback);
		}

		class SuggestCallback extends CancellableAsyncCallback<Response> {
			private Callback callback;

			private Request request;

			public SuggestCallback(Request request, Callback callback) {
				this.request = request;
				this.callback = callback;
			}

			@Override
			public void onFailure(Throwable caught) {
				throw new WrappedRuntimeException(caught);
			}

			@Override
			public void onSuccess(Response response) {
				if (!isCancelled()) {
					callback.onSuggestionsReady(request, response);
				}
			}
		}
	}

	public static class BoundSuggestOracleRequest extends Request {
		public BoundSuggestOracleModel model;

		public String targetClassName;

		public String hint;
	}

	@Override
	public int getTabIndex() {
		return base.getValueBox().getTabIndex();
	}

	@Override
	public void setAccessKey(char key) {
		base.getValueBox().setAccessKey(key);
	}

	@Override
	public void setFocus(boolean focused) {
		base.getValueBox().setFocus(focused);
	}

	@Override
	public void setTabIndex(int index) {
		base.getValueBox().setTabIndex(index);
	}
}
