package cc.alcina.framework.gwt.persistence.client.ios;

import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineTransitionHandler;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.gwt.client.logic.PossiblePanelProvider;
import cc.alcina.framework.gwt.persistence.client.PersistenceCallback;
import cc.alcina.framework.gwt.persistence.client.PersistenceCallback.PersistenceCallbackStd;
import cc.alcina.framework.gwt.persistence.client.PropertyStore;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HTML;

public class IosStorageExpander {
	public static final String CHECKED_KEY = IosStorageExpander.class.getName()
			+ "::CHECKED";

	public static final String EXPAND_KEY_PREFIX = IosStorageExpander.class
			.getName() + "::EXPANDCHUNK_";

	public static final String EXPAND_COUNT_KEY = IosStorageExpander.class
			.getName() + "::EXPANDCOUNT_";

	private State state = State.CHECK_DONE;

	int expansionCount = 0;

	protected Iterator<String> keysToDelete;

	private PossiblePanelProvider panelProvider;

	private AsyncCallback<Void> asyncCallback;

	public ExpandIosStorageHandler getTransitionHandler(
			PossiblePanelProvider panelProvider,
			AsyncCallback<Void> asyncCallback) {
		this.asyncCallback = asyncCallback;
		this.panelProvider = panelProvider;
		return new ExpandIosStorageHandler();
	}

	protected void handleException(Throwable caught) {
		throw new WrappedRuntimeException(caught);
	}

	void iterate() {
		AlcinaTopics.log(state);
		switch (state) {
		case CHECK_DONE:
			PropertyStore.get().get(CHECKED_KEY,
					new PersistenceCallback<String>() {
						@Override
						public void onFailure(Throwable caught) {
							handleException(caught);
						}

						@Override
						public void onSuccess(String result) {
							state = result == null ? State.SHOW_EXPAND_MESSAGE
									: State.DONE;
							iterate();
						}
					});
			break;
		case SHOW_EXPAND_MESSAGE:
			String msg = TextProvider
					.get()
					.getUiObjectText(
							IosStorageExpander.class,
							"Explanation",
							"Expanding storage for ios devices"
									+ " - please press \"OK\" when asked for more storage");
			HTML html = new HTML(msg);
			Style s = html.getElement().getStyle();
			s.setPadding(2, Unit.EM);
			s.setPaddingTop(4, Unit.EM);
			s.setProperty("textAlign", "center");
			ComplexPanel cp = panelProvider.providePanel();
			cp.add(html);
			state = State.EXPANDING_GET_COUNT;
			iterate();
			return;
		case EXPANDING_GET_COUNT:
			PropertyStore.get().get(EXPAND_COUNT_KEY,
					new PersistenceCallback<String>() {
						@Override
						public void onFailure(Throwable caught) {
							handleException(caught);
						}

						@Override
						public void onSuccess(String result) {
							expansionCount = result == null ? 0 : Integer
									.parseInt(result);
							if (expansionCount == 30) {
								state = State.EXPANDING_CLEAR_GET_KEYS;
							} else {
								state = State.EXPANDING_ADD;
							}
							iterate();
						}
					});
			break;
		case EXPANDING_ADD:
			String key = EXPAND_KEY_PREFIX + expansionCount;
			String value = "ab";
			int count = 18;// 2^20-1(len(ab)==2)-1(conservatively, sqllite
							// defaults to utf-8)
			for (int i = 0; i < count; i++) {
				value = value + value;
			}
			System.out.println("putting: " + expansionCount);
			PropertyStore.get().put(key, value,
					new PersistenceCallback<Integer>() {
						@Override
						public void onFailure(Throwable caught) {
							if (caught.getMessage().toLowerCase()
									.contains("storage quota")) {
								state = State.EXPANDING_INCREMENT_COUNT;
								iterate();
							} else {
								throw new WrappedRuntimeException(caught);
							}
						}

						@Override
						public void onSuccess(Integer result) {
							state = State.EXPANDING_INCREMENT_COUNT;
							iterate();
						}
					});
			break;
		case EXPANDING_INCREMENT_COUNT:
			expansionCount++;
			PropertyStore.get().put(EXPAND_COUNT_KEY,
					String.valueOf(expansionCount),
					new PersistenceCallback<Integer>() {
						@Override
						public void onFailure(Throwable caught) {
							handleException(caught);
						}

						@Override
						public void onSuccess(Integer result) {
							state = State.EXPANDING_GET_COUNT;
							iterate();
						}
					});
			break;
		case EXPANDING_CLEAR_GET_KEYS:
			PropertyStore.get().getKeysPrefixedBy(EXPAND_KEY_PREFIX,
					new PersistenceCallbackStd<List<String>>() {
						@Override
						public void onSuccess(List<String> result) {
							keysToDelete = result.iterator();
							state = State.EXPANDING_CLEAR_REMOVE_KEYS;
							iterate();
						}
					});
			break;
		case EXPANDING_CLEAR_REMOVE_KEYS:
			if (!keysToDelete.hasNext()) {
				PropertyStore.get().put(CHECKED_KEY, "true",
						new PersistenceCallback<Integer>() {
							@Override
							public void onFailure(Throwable caught) {
								handleException(caught);
							}

							@Override
							public void onSuccess(Integer result) {
								Window.alert(TextProvider.get()
										.getUiObjectText(
												IosStorageExpander.class,
												"expansion-complete",
												"Database expansion complete"));
								state = State.DONE;
								iterate();
							}
						});
				break;
			} else {
				PropertyStore.get().remove(keysToDelete.next(),
						new PersistenceCallbackStd() {
							@Override
							public void onSuccess(Object result) {
								iterate();
							}
						});
			}
			break;
		// case VACUUM:
		// ((ObjectStoreWebDbImpl) PropertyStore.get().getObjectStore())
		// .executeSql("VACUUM;",
		// new PersistenceCallbackStd<List<String>>() {
		// @Override
		// public void onSuccess(List<String> result) {
		// keysToDelete = result.iterator();
		// state = State.DONE;
		// iterate();
		// }
		// });
		// break;
		case DONE:
			asyncCallback.onSuccess(null);
			break;
		}
	}

	public class ExpandIosStorageHandler implements MachineTransitionHandler {
		@Override
		public void performTransition(MachineModel model) {
			iterate();
		}
	}

	private enum State {
		CHECK_DONE, SHOW_EXPAND_MESSAGE, EXPANDING_GET_COUNT, EXPANDING_ADD,
		EXPANDING_INCREMENT_COUNT, EXPANDING_CLEAR_GET_KEYS,
		EXPANDING_CLEAR_REMOVE_KEYS, DONE, VACUUM;
	}
}
