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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
public class DomainTransformRequest implements Serializable {
	private List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();

	private List<DomainTransformRequest> priorRequestsWithoutResponse = new ArrayList<DomainTransformRequest>();

	private int requestId;

	private Set<Long> eventIdsToIgnore = new HashSet<Long>();

	private ClientInstance clientInstance;

	private DomainTransformRequestType domainTransformRequestType;

	private String protocolVersion;

	private String tag;

	public List<DomainTransformEvent> allTransforms() {
		List<DomainTransformEvent> all = new ArrayList<DomainTransformEvent>();
		List<DomainTransformRequest> dtrs = allRequests();
		for (DomainTransformRequest dtr : dtrs) {
			all.addAll(dtr.getEvents());
		}
		return all;
	}

	private List<DomainTransformRequest> allRequests() {
		List<DomainTransformRequest> dtrs = new ArrayList<DomainTransformRequest>();
		dtrs.addAll(getPriorRequestsWithoutResponse());
		dtrs.add(this);
		return dtrs;
	}

	public void removeTransformsForObject(HasIdAndLocalId object) {
		for (DomainTransformRequest rq : allRequests()) {
			for (Iterator<DomainTransformEvent> itr = rq.getEvents().iterator(); itr
					.hasNext();) {
				DomainTransformEvent dte = itr.next();
				Object source = dte.provideSourceOrMarker();
				
				if (object.equals(source)) {
					itr.remove();
				}
			}
		}
	}

	public void fromString(String eventsStr) {
		new DTRProtocolSerializer().deserialize(this, getProtocolVersion(),
				eventsStr);
	}

	@Transient
	public ClientInstance getClientInstance() {
		return clientInstance;
	}

	public DomainTransformRequestType getDomainTransformRequestType() {
		return domainTransformRequestType;
	}

	@Transient
	public Set<Long> getEventIdsToIgnore() {
		return eventIdsToIgnore;
	}

	@Transient
	public List<DomainTransformEvent> getEvents() {
		return events;
	}

	/**
	 * Persistence defined in app subclass
	 */
	@Transient
	public List<DomainTransformRequest> getPriorRequestsWithoutResponse() {
		return this.priorRequestsWithoutResponse;
	}

	public void setPriorRequestsWithoutResponse(
			List<DomainTransformRequest> priorRequestsWithoutResponse) {
		this.priorRequestsWithoutResponse = priorRequestsWithoutResponse;
	}

	@Transient
	public String getProtocolVersion() {
		return protocolVersion;
	}

	public int getRequestId() {
		return requestId;
	}

	public String getTag() {
		return tag;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public void setDomainTransformRequestType(
			DomainTransformRequestType domainTransformRequestType) {
		this.domainTransformRequestType = domainTransformRequestType;
	}

	public void setEventIdsToIgnore(Set<Long> eventIdsToIgnore) {
		this.eventIdsToIgnore = eventIdsToIgnore;
	}

	public void setEvents(List<DomainTransformEvent> items) {
		this.events = items;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public void setRequestId(int eventId) {
		this.requestId = eventId;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String toString() {
		return new DTRProtocolSerializer()
				.serialize(this, getProtocolVersion());
	}

	public String toStringForError() {
		List<DomainTransformRequest> dtrs = allRequests();
		StringBuffer sb = new StringBuffer();
		for (DomainTransformRequest dtr : dtrs) {
			sb.append("----");
			sb.append(dtr.getRequestId());
			List<DomainTransformEvent> items = dtr.getEvents();
			for (DomainTransformEvent dte : items) {
				String s2 = "\t" + dte.toString().replace("\n", "\n\t") + "\n";
				sb.append(s2);
			}
		}
		return sb.toString();
	}

	public void updateTransformCommitType(CommitType commitType, boolean deep) {
		for (DomainTransformEvent dte : deep ? allTransforms() : getEvents()) {
			dte.setCommitType(commitType);
		}
	}

	public enum DomainTransformRequestType {
		TO_REMOTE, CLIENT_OBJECT_LOAD, CLIENT_SYNC, TO_REMOTE_COMPLETED,
		CLIENT_STARTUP_FROM_OFFLINE
	}
}
