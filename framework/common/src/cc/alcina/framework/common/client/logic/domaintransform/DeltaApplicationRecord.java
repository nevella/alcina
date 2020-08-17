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
package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.actions.RemoteParameters;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.gwittir.customiser.TextAreaCustomiser;

@Bean(displayNamePropertyName = "id")
@XmlRootElement
/**
 * TODO - this can either be a wrapper for a DomainTransformRequest, *or* a
 * gwt-rpc payload a lot of refactoring should be done to make that meaningful
 * 
 * @author Nick Reddel
 */
@ReflectionModule("Admin")
public class DeltaApplicationRecord extends Bindable
		implements RemoteParameters {
	private int id;

	private String text;

	private long timestamp;

	private long userId;

	private long clientInstanceId;

	private int requestId;

	private int clientInstanceAuth;

	private String protocolVersion;

	private DeltaApplicationRecordType type;

	private String tag;

	private String chunkUuidString;

	public DeltaApplicationRecord() {
	}

	public DeltaApplicationRecord(DomainTransformRequest request,
			DeltaApplicationRecordType type, boolean async) {
		this.timestamp = new Date().getTime();
		this.userId = PermissionsManager.get().getUserId();
		if (!async) {
			if (GWT.isClient()) {
				Registry.impl(ClientNotifications.class)
						.metricLogStart("DTRSimpleSerialWrapper-tostr");
				this.text = request.toString();
				Registry.impl(ClientNotifications.class)
						.metricLogEnd("DTRSimpleSerialWrapper-tostr");
			} else {
				this.text = request.toString();
			}
		} else {
			// text set async
		}
		this.clientInstanceId = request.getClientInstance().getId();
		this.requestId = request.getRequestId();
		Integer auth = request.getClientInstance().getAuth();
		this.clientInstanceAuth = auth == null ? 0 : auth;
		this.type = type;
		this.protocolVersion = request.getProtocolVersion();
		this.setTag(request.getTag());
		this.setChunkUuidString(request.getChunkUuidString());
	}

	public DeltaApplicationRecord(int id, String text, long timestamp,
			long userId, long clientInstanceId, int requestId,
			int clientInstanceAuth, DeltaApplicationRecordType type,
			String protocolVersion, String tag, String chunkUuidString) {
		this.id = id;
		this.text = text;
		this.timestamp = timestamp;
		this.userId = userId;
		this.clientInstanceId = clientInstanceId;
		this.requestId = requestId;
		this.clientInstanceAuth = clientInstanceAuth;
		this.type = type;
		this.protocolVersion = protocolVersion;
		this.setTag(tag);
		this.setChunkUuidString(chunkUuidString);
	}

	public DeltaApplicationRecord copy() {
		return new DeltaApplicationRecord(id, text, timestamp, userId,
				clientInstanceId, requestId, clientInstanceAuth, type,
				protocolVersion, tag, chunkUuidString);
	}

	public String getChunkUuidString() {
		return this.chunkUuidString;
	}

	@Display(name = "Client instance auth", orderingHint = 30)
	public int getClientInstanceAuth() {
		return clientInstanceAuth;
	}

	@Display(name = "Client instance id", orderingHint = 20)
	public long getClientInstanceId() {
		return this.clientInstanceId;
	}

	public int getId() {
		return this.id;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	@Display(name = "Request id", orderingHint = 10)
	public int getRequestId() {
		return this.requestId;
	}

	@Display(name = "Tag", orderingHint = 35)
	public String getTag() {
		return tag;
	}

	@Display(name = "Transforms", orderingHint = 40)
	@Custom(customiserClass = TextAreaCustomiser.class, parameters = {
			@NamedParameter(name = TextAreaCustomiser.LINES, intValue = 10),
			@NamedParameter(name = TextAreaCustomiser.WIDTH, intValue = 400) })
	public String getText() {
		return this.text;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public DeltaApplicationRecordType getType() {
		return this.type;
	}

	public long getUserId() {
		return this.userId;
	}

	public void setChunkUuidString(String chunkUuidString) {
		this.chunkUuidString = chunkUuidString;
	}

	public void setClientInstanceAuth(int clientInstanceAuth) {
		int old_clientInstanceAuth = this.clientInstanceAuth;
		this.clientInstanceAuth = clientInstanceAuth;
		propertyChangeSupport().firePropertyChange("clientInstanceAuth",
				old_clientInstanceAuth, clientInstanceAuth);
	}

	public void setClientInstanceId(long clientInstanceId) {
		long old_clientInstanceId = this.clientInstanceId;
		this.clientInstanceId = clientInstanceId;
		propertyChangeSupport().firePropertyChange("clientInstanceId",
				old_clientInstanceId, clientInstanceId);
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setProtocolVersion(String protocolVersion) {
		String old_protocolVersion = this.protocolVersion;
		this.protocolVersion = protocolVersion;
		propertyChangeSupport().firePropertyChange("protocolVersion",
				old_protocolVersion, protocolVersion);
	}

	public void setRequestId(int requestId) {
		int old_requestId = this.requestId;
		this.requestId = requestId;
		propertyChangeSupport().firePropertyChange("requestId", old_requestId,
				requestId);
	}

	public void setTag(String tag) {
		String old_tag = this.tag;
		this.tag = tag;
		propertyChangeSupport().firePropertyChange("tag", old_tag, tag);
	}

	public void setText(String text) {
		String old_text = this.text;
		this.text = text;
		propertyChangeSupport().firePropertyChange("text", old_text, text);
	}

	public void setTimestamp(long timestamp) {
		long old_timestamp = this.timestamp;
		this.timestamp = timestamp;
		propertyChangeSupport().firePropertyChange("timestamp", old_timestamp,
				timestamp);
	}

	public void setType(DeltaApplicationRecordType type) {
		DeltaApplicationRecordType old_type = this.type;
		this.type = type;
		propertyChangeSupport().firePropertyChange("type", old_type, type);
	}

	public void setUserId(long userId) {
		long old_userId = this.userId;
		this.userId = userId;
		propertyChangeSupport().firePropertyChange("userId", old_userId,
				userId);
	}

	@Override
	public String toString() {
		return Ax.format(" clientInstanceAuth: %s\n" + "clientInstanceId: %s\n"
				+ "id: %s\n" + "requestId: %s\n" + "timestamp: %s\n"
				+ "userId: %s\n" + "DeltaApplicationRecordType: %s\n"
				+ "tag:\n%s\n" + "chunkUuidString:\n%s\n" + "text:\n%s\n",
				clientInstanceAuth, clientInstanceId, id, requestId, timestamp,
				userId, type, getTag(), chunkUuidString, text);
	}
}