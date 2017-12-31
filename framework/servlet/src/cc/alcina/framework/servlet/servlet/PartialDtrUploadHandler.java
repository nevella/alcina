package cc.alcina.framework.servlet.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadResponse;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DeltaApplicationRecordSerializerImpl;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.servlet.ServletLayerObjects;

public class PartialDtrUploadHandler {
	public PartialDtrUploadResponse uploadOfflineTransforms(
			PartialDtrUploadRequest request,
			CommonRemoteServiceServlet commonRemoteServiceServlet)
			throws WebException {
		try {
			PartialDtrUploadResponse response = new PartialDtrUploadResponse();
			File dataFolder = ServletLayerObjects.get().getDataFolder();
			File dir = new File(dataFolder.getPath() + File.separator
					+ "offlineTransforms-partial");
			dir.mkdirs();
			CommonPersistenceLocal cp = Registry
					.impl(CommonPersistenceProvider.class)
					.getCommonPersistence();
			Class<? extends ClientInstance> clientInstanceClass = cp
					.getImplementation(ClientInstance.class);
			DeltaApplicationRecord firstWrapper = request.wrappers.get(0);
			ClientInstance clientInstance = clientInstanceClass.newInstance();
			clientInstance.setAuth(firstWrapper.getClientInstanceAuth());
			final long clientInstanceId = firstWrapper.getClientInstanceId();
			clientInstance.setId(clientInstanceId);
			ClientInstance persistentClientInstance = (ClientInstance) cp
					.findImplInstance(ClientInstance.class, clientInstanceId);
			if (persistentClientInstance == null || !persistentClientInstance
					.getAuth().equals(clientInstance.getAuth())) {
				// throw new WebException(
				// "Invalid authentication/client instance id in offline
				// upload");
			}
			final Pattern fnP = Pattern.compile("(\\d+)_(\\d+)_ser.txt");
			File[] list = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					Matcher m = fnP.matcher(name);
					return m.matches()
							&& Long.parseLong(m.group(1)) == clientInstanceId;
				}
			});
			TreeMap<Integer, File> rqs = new TreeMap<Integer, File>();
			TreeMap<Integer, DeltaApplicationRecord> fullWrappers = new TreeMap<Integer, DeltaApplicationRecord>();
			DeltaApplicationRecordSerializerImpl dtrSerializer = new DeltaApplicationRecordSerializerImpl();
			for (File file : list) {
				Matcher m = fnP.matcher(file.getName());
				m.matches();
				int id = Integer.parseInt(m.group(2));
				rqs.put(id, file);
				String ser = ResourceUtilities.readFileToString(rqs.get(id));
				DeltaApplicationRecord wrapper = dtrSerializer.read(ser);
				DomainTransformRequest rq = new DomainTransformRequest();
				rq.fromString(wrapper.getText());
				fullWrappers.put(id, wrapper);
				response.transformsUploadedButNotCommitted += rq.getEvents()
						.size();
			}
			for (int i = 0; i < request.wrappers.size(); i++) {
				DeltaApplicationRecord wrapper = request.wrappers.get(i);
				List<DomainTransformEvent> transforms = request.transformLists
						.get(i);
				int id = wrapper.getRequestId();
				if (rqs.containsKey(id)) {
					String ser = ResourceUtilities
							.readFileToString(rqs.get(id));
					wrapper = dtrSerializer.read(ser);
				}
				DomainTransformRequest rq = new DomainTransformRequest();
				rq.fromString(wrapper.getText());
				rq.getEvents().addAll(transforms);
				response.transformsUploadedButNotCommitted += transforms.size();
				wrapper.setText(rq.toString());
				String fileName = String.format("%s_%s_ser.txt",
						clientInstanceId, id);
				String path = dir.getPath() + File.separator + fileName;
				File file = new File(path);
				ResourceUtilities
						.writeStringToFile(dtrSerializer.write(wrapper), file);
				rqs.put(id, file);
				fullWrappers.put(id, wrapper);
			}
			String ser = ResourceUtilities
					.readFileToString(rqs.lastEntry().getValue());
			DomainTransformRequest rq = new DomainTransformRequest();
			DeltaApplicationRecord wrapper = dtrSerializer.read(ser);
			rq.fromString(wrapper.getText());
			response.lastUploadedRequestId = wrapper.getRequestId();
			response.lastUploadedRequestTransformUploadCount = rq.getEvents()
					.size();
			if (request.commitOnReceipt) {
				try {
					commonRemoteServiceServlet.persistOfflineTransforms(
							new ArrayList<DeltaApplicationRecord>(
									fullWrappers.values()));
				} catch (Exception e) {
					CommonPersistenceLocal cpl = Registry
							.impl(CommonPersistenceProvider.class)
							.getCommonPersistence();
					String errMsg = String.format(
							"Client instance id:%s - Partial dtr upload - %s \nAll request ids: %s",
							clientInstanceId, response, fullWrappers.keySet());
					cpl.log(errMsg,
							LogMessageType.OFFLINE_TRANSFORM_MERGE_EXCEPTION
									.toString());
					throw e;
				}
				response.committed = true;
			}
			ServletLayerObjects.get().getMetricLogger()
					.info(String.format(
							"Client instance id:%s - Partial dtr upload - %s",
							clientInstanceId, response));
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebException(e);
		}
	}
}
