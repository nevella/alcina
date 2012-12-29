package cc.alcina.framework.gwt.persistence.client.ios;

import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineTransitionHandler;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.gwt.client.logic.PossiblePanelProvider;
import cc.alcina.framework.gwt.persistence.client.ObjectStoreWebDbImpl;
import cc.alcina.framework.gwt.persistence.client.PersistenceCallback;
import cc.alcina.framework.gwt.persistence.client.PersistenceCallback.PersistenceCallbackStd;
import cc.alcina.framework.gwt.persistence.client.PropertyStore;

import com.google.code.gwt.database.client.Database;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HTML;

public class IosStorageExpander {
	private static final int EXPAND_TO_MB_MULTIPLES = 25;

	// app cache will give us about 7
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

	private Database db;

	private PropertyStore tmpPropertyStore;

	private ObjectStoreWebDbImpl objectStore;

	private ComplexPanel cp;

	public ExpandIosStorageHandler getTransitionHandler(
			PossiblePanelProvider panelProvider,
			AsyncCallback<Void> asyncCallback, Database db) {
		this.asyncCallback = asyncCallback;
		this.panelProvider = panelProvider;
		this.db = db;
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
							state = result == null ? State.INIT_TMP_STORE
									: State.DONE;
							iterate();
						}
					});
			break;
		case INIT_TMP_STORE:
			PersistenceCallback<Void> itrCallback = new PersistenceCallback<Void>() {
				@Override
				public void onFailure(Throwable caught) {
					handleException(caught);
				}

				@Override
				public void onSuccess(Void result) {
					tmpPropertyStore = PropertyStore
							.createNonStandardPropertyStore(objectStore);
					state = State.SHOW_EXPAND_MESSAGE;
					iterate();
				}
			};
			objectStore = new ObjectStoreWebDbImpl(db, "tmp_propertyStore",
					itrCallback);
			break;
		case SHOW_EXPAND_MESSAGE:
			String msg = TextProvider
					.get()
					.getUiObjectText(
							IosStorageExpander.class,
							"Explanation",
							"Expanding storage for ios devices"
									+ " - please press \"Increase\" when asked for more storage");
			HTML html = new HTML(msg);
			Style s = html.getElement().getStyle();
			s.setPadding(2, Unit.EM);
			s.setPaddingTop(4, Unit.EM);
			s.setProperty("textAlign", "center");
			cp = panelProvider.providePanel();
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
							if (expansionCount == EXPAND_TO_MB_MULTIPLES) {
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
			tmpPropertyStore.put(key, value,
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
			tmpPropertyStore.getKeysPrefixedBy(EXPAND_KEY_PREFIX,
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
								state = State.DROP;
								iterate();
							}
						});
				break;
			} else {
				tmpPropertyStore.remove(keysToDelete.next(),
						new PersistenceCallbackStd() {
							@Override
							public void onSuccess(Object result) {
								iterate();
							}
						});
			}
			break;
		case DROP:
			objectStore.drop(new PersistenceCallback<Void>() {
				@Override
				public void onFailure(Throwable caught) {
					handleException(caught);
				}

				@Override
				public void onSuccess(Void result) {
					state = State.DONE;
					iterate();
				}
			});
			// case VACUUM:
			// ((ObjectStoreWebDbImpl) propertyStore.getObjectStore())
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
			if (cp != null) {
				cp.removeFromParent();
			}
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
		INIT_TMP_STORE, CHECK_DONE, SHOW_EXPAND_MESSAGE, EXPANDING_GET_COUNT,
		EXPANDING_ADD, EXPANDING_INCREMENT_COUNT, EXPANDING_CLEAR_GET_KEYS,
		EXPANDING_CLEAR_REMOVE_KEYS, DROP, DONE, VACUUM;
	}
}
