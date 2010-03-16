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

package cc.alcina.framework.gwt.client.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.StateListenable;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransform.DataTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransform.DataTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest.DataTransformRequestType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 *
 * @author Nick Reddel
 */

 public class CommitToServerTransformListener extends StateListenable implements
		DataTransformListener {
	public static final int DELAY_MS = 100;

	private List<DataTransformEvent> transformQueue;

	private List<DataTransformRequest> priorRequestsWithoutResponse = new ArrayList<DataTransformRequest>();

	public List<DataTransformRequest> getPriorRequestsWithoutResponse() {
		return this.priorRequestsWithoutResponse;
	}

	private Timer queueingFinishedTimer;

	private long lastQueueAddMillis;

	private boolean suppressErrors = false;

	private int localRequestId = 1;

	public int getLocalRequestId() {
		return this.localRequestId;
	}

	public void setLocalRequestId(int localRequestId) {
		this.localRequestId = localRequestId;
	}

	private ClientInstance clientInstance;

	public CommitToServerTransformListener() {
		resetQueue();
	}

	private void resetQueue() {
		transformQueue = new ArrayList<DataTransformEvent>();
	}

	public void dataTransform(DataTransformEvent evt) {
		if (evt.getCommitType() == CommitType.TO_REMOTE_STORAGE) {
			TransformManager tm = TransformManager.get();
			if (tm.isReplayingRemoteEvent()) {
				return;
			}
			transformQueue.add(evt);
			lastQueueAddMillis = new Date().getTime();
			if (queueingFinishedTimer == null) {
				queueingFinishedTimer = new Timer() {
					long timerAddedMillis = lastQueueAddMillis;

					@Override
					public void run() {
						if (lastQueueAddMillis - timerAddedMillis == 0) {
							commit();
						}
						timerAddedMillis = lastQueueAddMillis;
					}
				};
				queueingFinishedTimer.scheduleRepeating(DELAY_MS);
			}
			return;
		}
	}

	public void flush() {
		commit();
		priorRequestsWithoutResponse.clear();
	}

	/*
	 * Unimplemented for the moment. This may or may not be necessary to
	 * accelerate change conflict checking
	 */
	void updateTransformQueueVersions() {
	}

	private Map<Long, Long> localToServerIds = new HashMap<Long, Long>();

	 ArrayList<DataTransformEvent> synthesisedEvents;

	public ArrayList<DataTransformEvent> getSynthesisedEvents() {
		return this.synthesisedEvents;
	}

	public Long localToServerId(Long localId) {
		return localToServerIds.get(localId);
	}

	void commit() {
		if (priorRequestsWithoutResponse.size() == 0
				&& transformQueue.size() == 0) {
			return;
		}
		if (queueingFinishedTimer != null) {
			queueingFinishedTimer.cancel();
		}
		queueingFinishedTimer = null;
		final DataTransformRequest dtr = new DataTransformRequest();
		dtr.getPriorRequestsWithoutResponse().addAll(
				priorRequestsWithoutResponse);
		dtr.setRequestId(localRequestId++);
		
		dtr.setClientInstance(clientInstance);
		dtr.getItems().addAll(transformQueue);
		dtr.setDataTransformRequestType(DataTransformRequestType.TO_REMOTE);
		updateTransformQueueVersions();
		resetQueue();
		AsyncCallback<DataTransformResponse> callback = new AsyncCallback<DataTransformResponse>() {
			public void onFailure(Throwable caught) {
				if (!suppressErrors) {
					throw new WrappedRuntimeException(caught);
				}
				fireStateChanged(ERROR);
			}

			public void onSuccess(DataTransformResponse response) {
				PermissionsManager.get().setOnlineState(OnlineState.ONLINE);
				TransformManager tm = TransformManager.get();
				tm.setReplayingRemoteEvent(true);
				try {
					 synthesisedEvents = new ArrayList<DataTransformEvent>();
					for (DataTransformEvent dte : response
							.getEventsToUseForClientUpdate()) {
						long id = dte.getGeneratedServerId() != 0 ? dte
								.getGeneratedServerId() : dte.getObjectId();
						if (dte.getGeneratedServerId() != 0) {
							DataTransformEvent idEvt = new DataTransformEvent();
							idEvt.setObjectClass(dte.getObjectClass());
							idEvt.setObjectLocalId(dte.getObjectLocalId());
							idEvt
									.setPropertyName(TransformManager.ID_FIELD_NAME);
							idEvt.setValueClass(Long.class);
							idEvt
									.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
							idEvt.setNewStringValue(String.valueOf(id));
							synthesisedEvents.add(idEvt);
							idEvt = new DataTransformEvent();
							idEvt.setObjectClass(dte.getObjectClass());
							idEvt.setObjectId(id);
							idEvt
									.setPropertyName(TransformManager.LOCAL_ID_FIELD_NAME);
							idEvt.setNewStringValue("0");
							idEvt.setValueClass(Long.class);
							idEvt
									.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
							synthesisedEvents.add(idEvt);
							localToServerIds.put(dte.getObjectLocalId(), id);
						}
						if (dte.getObjectVersionNumber() != 0) {
							DataTransformEvent idEvt = new DataTransformEvent();
							idEvt.setObjectClass(dte.getObjectClass());
							idEvt.setObjectId(id);
							idEvt
									.setPropertyName(TransformManager.VERSION_FIELD_NAME);
							idEvt.setNewStringValue(String.valueOf(dte
									.getObjectVersionNumber()));
							idEvt.setValueClass(Integer.class);
							idEvt
									.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
							synthesisedEvents.add(idEvt);
						}
					}
					for (DataTransformEvent dte : synthesisedEvents) {
						try {
							tm.consume(dte);
							tm.fireDataTransform(dte);// this notifies
							// gears?
							// well, definitely notifies clients who need to
							// know chanages were committed
						} catch (DataTransformException e) {
							// shouldn't happen
							throw new WrappedRuntimeException(e);
						}
					}
					List<DataTransformEvent> items = dtr.getItems();
					for (DataTransformEvent evt : items) {
						TransformManager.get().setTransformCommitType(evt,
								CommitType.ALL_COMMITTED);
					}
					for (int i = priorRequestsWithoutResponse.size() - 1; i >= 0; i--) {
						if (priorRequestsWithoutResponse.get(i).getRequestId() <= dtr
								.getRequestId()) {
							priorRequestsWithoutResponse.remove(i);
						}
					}
				} finally {
					tm.setReplayingRemoteEvent(false);
					fireStateChanged(COMMITTED);
				}
			}
		};
		if (clientInstance==null){
			int j=3;
		}
		IUser user = clientInstance.getUser();
		//not needed, and heavyweight
		clientInstance.setUser(null);
		ClientLayerLocator.get().commonRemoteServiceAsync().transform(dtr, callback);
		clientInstance.setUser(user);
		clientInstance.setUser(null);
		dtr.getPriorRequestsWithoutResponse().clear();
		priorRequestsWithoutResponse.add(dtr);
		fireStateChanged(COMMITTING);
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public ClientInstance getClientInstance() {
		return clientInstance;
	}

	public void setSuppressErrors(boolean suppressErrors) {
		this.suppressErrors = suppressErrors;
	}

	public boolean isSuppressErrors() {
		return suppressErrors;
	}

	public static final String COMMITTING = "COMMITTING";

	public static final String COMMITTED = "COMMITTED";

	public static final String ERROR = "ERROR";
}