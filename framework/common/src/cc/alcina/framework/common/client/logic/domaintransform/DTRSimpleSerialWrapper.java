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
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.gwittir.customiser.TextAreaCustomiser;

@BeanInfo(displayNamePropertyName = "id")
@XmlRootElement
/**
 *
 * @author Nick Reddel
 */
public class DTRSimpleSerialWrapper extends BaseBindable implements
		RemoteParameters {
	private int id;

	private String text;

	private long timestamp;

	private long userId;

	private long clientInstanceId;

	private int requestId;

	private int clientInstanceAuth;

	private String protocolVersion;

	private DomainTransformRequestType domainTransformRequestType;

	public DTRSimpleSerialWrapper() {
	}

	public DTRSimpleSerialWrapper(DomainTransformRequest request) {
		this(request, false);
	}

	public DTRSimpleSerialWrapper(DomainTransformRequest request, boolean async) {
		this.timestamp = new Date().getTime();
		this.userId = PermissionsManager.get().getUserId();
		if (!async) {
			ClientLayerLocator.get().clientBase().metricLogStart(
					"DTRSimpleSerialWrapper-tostr");
			this.text = request.toString();
			ClientLayerLocator.get().clientBase().metricLogEnd(
					"DTRSimpleSerialWrapper-tostr");
		} else {
			//text set async
		}
		this.clientInstanceId = request.getClientInstance().getId();
		this.requestId = request.getRequestId();
		Integer auth = request.getClientInstance().getAuth();
		this.clientInstanceAuth = auth == null ? 0 : auth;
		this.domainTransformRequestType = request
				.getDomainTransformRequestType();
		this.protocolVersion=request.getProtocolVersion();
	}

	@Override
	public String toString() {
		return CommonUtils.format(" clientInstanceAuth: %1\n"
				+ "clientInstanceId: %2\n" + "id: %3\n" + "requestId: %4\n"
				+ "timestamp: %5\n" + "userId: %6\n"
				+ "DomainTransformRequestType: %7\n" + "text:\n%8\n",
				clientInstanceAuth, clientInstanceId, id, requestId, timestamp,
				userId, domainTransformRequestType, text);
	}

	public DTRSimpleSerialWrapper(int id, String text, long timestamp,
			long userId, long clientInstanceId, int requestId,
			int clientInstanceAuth,
			DomainTransformRequestType domainTransformRequestType, String protocolVersion) {
		this.text = text;
		this.timestamp = timestamp;
		this.userId = userId;
		this.clientInstanceId = clientInstanceId;
		this.id = id;
		this.requestId = requestId;
		this.clientInstanceAuth = clientInstanceAuth;
		this.domainTransformRequestType = domainTransformRequestType;
		this.protocolVersion = protocolVersion;
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Client instance auth", orderingHint = 30))
	public int getClientInstanceAuth() {
		return clientInstanceAuth;
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Client instance id", orderingHint = 20))
	public long getClientInstanceId() {
		return this.clientInstanceId;
	}

	public DomainTransformRequestType getDomainTransformRequestType() {
		return domainTransformRequestType;
	}

	public int getId() {
		return this.id;
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Request id", orderingHint = 10))
	public int getRequestId() {
		return this.requestId;
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Transforms", orderingHint = 40))
	@CustomiserInfo(customiserClass = TextAreaCustomiser.class, parameters = {
			@NamedParameter(name = TextAreaCustomiser.LINES, intValue = 10),
			@NamedParameter(name = TextAreaCustomiser.WIDTH, intValue = 400) })
	public String getText() {
		return this.text;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public long getUserId() {
		return this.userId;
	}

	public void setClientInstanceAuth(int clientInstanceAuth) {
		int old_clientInstanceAuth = this.clientInstanceAuth;
		this.clientInstanceAuth = clientInstanceAuth;
		propertyChangeSupport.firePropertyChange("clientInstanceAuth",
				old_clientInstanceAuth, clientInstanceAuth);
	}

	public void setClientInstanceId(long clientInstanceId) {
		long old_clientInstanceId = this.clientInstanceId;
		this.clientInstanceId = clientInstanceId;
		propertyChangeSupport.firePropertyChange("clientInstanceId",
				old_clientInstanceId, clientInstanceId);
	}

	public void setDomainTransformRequestType(
			DomainTransformRequestType domainTransformRequestType) {
		DomainTransformRequestType old_domainTransformRequestType = this.domainTransformRequestType;
		this.domainTransformRequestType = domainTransformRequestType;
		propertyChangeSupport.firePropertyChange("domainTransformRequestType",
				old_domainTransformRequestType, domainTransformRequestType);
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setRequestId(int requestId) {
		int old_requestId = this.requestId;
		this.requestId = requestId;
		propertyChangeSupport.firePropertyChange("requestId", old_requestId,
				requestId);
	}

	public void setText(String text) {
		String old_text = this.text;
		this.text = text;
		propertyChangeSupport.firePropertyChange("text", old_text, text);
	}

	public void setTimestamp(long timestamp) {
		long old_timestamp = this.timestamp;
		this.timestamp = timestamp;
		propertyChangeSupport.firePropertyChange("timestamp", old_timestamp,
				timestamp);
	}

	public void setUserId(long userId) {
		long old_userId = this.userId;
		this.userId = userId;
		propertyChangeSupport.firePropertyChange("userId", old_userId, userId);
	}

	public void setProtocolVersion(String protocolVersion) {
		String old_protocolVersion = this.protocolVersion;
		this.protocolVersion = protocolVersion;
		propertyChangeSupport.firePropertyChange("protocolVersion",
				old_protocolVersion, protocolVersion);
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}
}