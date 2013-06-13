package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadResponse;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;

public class PartialDtrUploader {
	private List<DTRSimpleSerialWrapper> uncommitted;

	private ModalNotifier modalNotifier;

	private AsyncCallback<Void> postPersistOfflineTransformsCallback;

	private Map<DTRSimpleSerialWrapper, List<DomainTransformEvent>> deserTransforms = new LinkedHashMap<DTRSimpleSerialWrapper, List<DomainTransformEvent>>();

	protected static int MIN_SLICE_SIZE = 500;

	protected static int MAX_SLICE_SIZE = 10000;

	protected static int INITIAL_SLICE_SIZE = 1500;

	protected static int MAX_COMFORTABLE_ROUNDTRIP_MS_DEFAULT = 10000;

	protected static int MAX_ERRORS_PER_REQUEST = 5;

	private int errorCountCurrentRequest;

	@SuppressWarnings("unused")
	private int errorCountCurrentTotal;

	private int currentSliceSize = INITIAL_SLICE_SIZE;

	private int totalTransforms;

	private int committedTransforms;

	protected PartialDtrUploadResponse currentResponse;

	private Set<Long> clientInstanceIds = new TreeSet<Long>();

	private AsyncCallback<PartialDtrUploadResponse> responseHandler = new AsyncCallback<PartialDtrUploadResponse>() {
		@Override
		public void onSuccess(PartialDtrUploadResponse response) {
			//don't turn on until first response - may be offline
			modalNotifier.modalOn();
			if (response.committed) {
				postPersistOfflineTransformsCallback.onSuccess(null);
				return;
			}
			currentResponse = response;
			committedTransforms = response.transformsUploadedButNotCommitted;
			modalNotifier.setProgress(((double) committedTransforms)
					/ ((double) totalTransforms) * 0.9);
			if (currentRequest.hasTransforms()) {
				long roundtripTime = System.currentTimeMillis() - submitTime;
				if (roundtripTime > MAX_COMFORTABLE_ROUNDTRIP_MS_DEFAULT) {
					modifySliceSize(0.5);
				} else if (roundtripTime < MAX_COMFORTABLE_ROUNDTRIP_MS_DEFAULT * 2) {
					modifySliceSize(2);
				}
			}
			generateRequest();
			submit(currentRequest);
		}

		@Override
		public void onFailure(Throwable caught) {
			if (caught instanceof StatusCodeException) {
				errorCountCurrentRequest++;
				errorCountCurrentTotal++;
				if (errorCountCurrentRequest < MAX_ERRORS_PER_REQUEST) {
					if (currentRequest.hasTransforms()) {
						modifySliceSize(0.5);
						generateRequest();
					}
					submit0();
					return;
				}
			}
			fatal(caught);
		}
	};

	private PartialDtrUploadRequest currentRequest;

	private long submitTime;

	public void persistOfflineTransforms(
			List<DTRSimpleSerialWrapper> uncommitted,
			ModalNotifier modalNotifier,
			AsyncCallback<Void> postPersistOfflineTransformsCallback) {
		this.uncommitted = uncommitted;
		this.modalNotifier = modalNotifier;
		this.postPersistOfflineTransformsCallback = postPersistOfflineTransformsCallback;
		for (DTRSimpleSerialWrapper wrapper : uncommitted) {
			DomainTransformRequest rq = new DomainTransformRequest();
			rq.setProtocolVersion(wrapper.getProtocolVersion());
			rq.fromString(wrapper.getText());
			clientInstanceIds.add(wrapper.getClientInstanceId());
			deserTransforms.put(wrapper, rq.getEvents());
			totalTransforms += rq.getEvents().size();
		}
		PartialDtrUploadRequest request = new PartialDtrUploadRequest();
		request.pleaseProvideCurrentStatus = true;
		addToRequest(request, uncommitted.get(0),
				new ArrayList<DomainTransformEvent>());
		submit(request);
	}

	protected void fatal(Throwable caught) {
		postPersistOfflineTransformsCallback.onFailure(caught);
	}

	protected void generateRequest() {
		assert currentResponse != null;
		PartialDtrUploadRequest request = new PartialDtrUploadRequest();
		currentRequest = request;
		int transformsInRequest = 0;
		boolean foundStart = false;
		for (int i = 0; i < uncommitted.size(); i++) {
			DTRSimpleSerialWrapper wrapper = uncommitted.get(i);
			if (wrapper.getRequestId() < currentResponse.lastUploadedRequestId) {
				continue;
			}
			int startIndex = wrapper.getRequestId() == currentResponse.lastUploadedRequestId ? currentResponse.lastUploadedRequestTransformUploadCount
					: 0;
			List<DomainTransformEvent> transforms = deserTransforms
					.get(wrapper);
			if (startIndex == transforms.size()) {
				continue;
				// wrapper all uploaded, start from next
			}
			int length = Math.min(transforms.size() - startIndex,
					currentSliceSize - transformsInRequest);
			addToRequest(request, wrapper, new ArrayList<DomainTransformEvent>(
					transforms.subList(startIndex, startIndex + length)));
			transformsInRequest += length;
			if (transformsInRequest == currentSliceSize) {
				break;
			}
		}
		if (request.wrappers.isEmpty()) {
			// add a blank request (no transforms) for auth
			addToRequest(currentRequest, uncommitted.get(0),
					new ArrayList<DomainTransformEvent>());
			LooseContext
					.getContext()
					.setBoolean(
							LocalTransformPersistence.CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED);
			LooseContext
					.getContext()
					.set(LocalTransformPersistence.CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED_CLIENT_IDS,
							CommonUtils.join(clientInstanceIds, ","));
			request.commitOnReceipt = true;
			String message = TextProvider.get().getUiObjectText(
					PartialDtrUploader.class, "committing",
					"Changes uploaded, processing on server");
			modalNotifier.setStatus(message);
		}
	}

	protected void modifySliceSize(double d) {
		currentSliceSize = (int) Math.round(currentSliceSize * d);
		currentSliceSize = Math.min(currentSliceSize, MAX_SLICE_SIZE);
		currentSliceSize = Math.max(currentSliceSize, MIN_SLICE_SIZE);
	}

	private void submit(PartialDtrUploadRequest request) {
		errorCountCurrentRequest = 0;
		currentRequest = request;
		submit0();
	}

	private void submit0() {
		submitTime = System.currentTimeMillis();
		ClientLayerLocator.get().commonRemoteServiceAsyncInstance()
				.uploadOfflineTransforms(currentRequest, responseHandler);
	}

	void addToRequest(PartialDtrUploadRequest request,
			DTRSimpleSerialWrapper wrapper,
			List<DomainTransformEvent> transforms) {
		request.transformLists.add(transforms);
		DTRSimpleSerialWrapper clone = wrapper.clone();
		clone.setText("");
		request.wrappers.add(clone);
	}
}
