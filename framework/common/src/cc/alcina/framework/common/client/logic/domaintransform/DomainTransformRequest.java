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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
public class DomainTransformRequest implements Serializable {
	public static List<DomainTransformEvent>
			allEvents(Collection<? extends DomainTransformRequest> requests) {
		List<DomainTransformEvent> result = new ArrayList<DomainTransformEvent>();
		for (DomainTransformRequest request : requests) {
			result.addAll(request.getEvents());
		}
		return result;
	}

	public static boolean checkSequential(List<DomainTransformEvent> events) {
		long lastLocalId = -1;
		for (DomainTransformEvent dte : events) {
			if (dte.getTransformType() == TransformType.CREATE_OBJECT) {
				if (lastLocalId != -1
						&& dte.getObjectLocalId() - lastLocalId != 1) {
					return false;
				}
				lastLocalId = dte.getObjectLocalId();
			}
		}
		return true;
	}

	private List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();

	private List<DomainTransformRequest> priorRequestsWithoutResponse = new ArrayList<DomainTransformRequest>();

	private int requestId;

	private Set<Long> eventIdsToIgnore = new HashSet<Long>();

	private ClientInstance clientInstance;

	private String protocolVersion;

	private String tag;

	@Transient
	public Map<String, String> properties;

	public List<DomainTransformEvent> allTransforms() {
		List<DomainTransformEvent> all = new ArrayList<DomainTransformEvent>();
		List<DomainTransformRequest> dtrs = allRequests();
		for (DomainTransformRequest dtr : dtrs) {
			all.addAll(dtr.getEvents());
		}
		return all;
	}

	public boolean checkForDuplicateEvents() {
		Set<Long> createIds = new LinkedHashSet<Long>();
		boolean duplicates = false;
		for (Iterator<DomainTransformEvent> itr = events.iterator(); itr
				.hasNext();) {
			DomainTransformEvent event = itr.next();
			if (event.getTransformType() == TransformType.CREATE_OBJECT) {
				if (!createIds.add(event.getObjectLocalId())) {
					itr.remove();
					duplicates = true;
				}
			}
		}
		return duplicates;
	}

	public void fromString(String eventsStr) {
		new DTRProtocolSerializer().deserialize(this, getProtocolVersion(),
				eventsStr);
	}

	@Transient
	public ClientInstance getClientInstance() {
		return clientInstance;
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

	public void removeTransform(DomainTransformEvent dte) {
		for (DomainTransformRequest rq : allRequests()) {
			rq.getEvents().removeIf(e -> e == dte);
		}
	}

	public void removeTransformsForObject(HasIdAndLocalId object) {
		for (DomainTransformRequest rq : allRequests()) {
			for (Iterator<DomainTransformEvent> itr = rq.getEvents()
					.iterator(); itr.hasNext();) {
				DomainTransformEvent dte = itr.next();
				Object source = dte.provideSourceOrMarker();
				if (object.equals(source)) {
					itr.remove();
				}
			}
		}
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public void setEventIdsToIgnore(Set<Long> eventIdsToIgnore) {
		this.eventIdsToIgnore = eventIdsToIgnore;
	}

	public void setEvents(List<DomainTransformEvent> items) {
		this.events = items;
	}

	public void setPriorRequestsWithoutResponse(
			List<DomainTransformRequest> priorRequestsWithoutResponse) {
		this.priorRequestsWithoutResponse = priorRequestsWithoutResponse;
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

	public String shortId() {
		return CommonUtils.formatJ("Dtr: cli-id: %s - rq-id: %s",
				HiliHelper.getIdOrNull(clientInstance), requestId);
	}

	public String toString() {
		return new DTRProtocolSerializer().serialize(this,
				getProtocolVersion());
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

	private List<DomainTransformRequest> allRequests() {
		List<DomainTransformRequest> dtrs = new ArrayList<DomainTransformRequest>();
		dtrs.addAll(getPriorRequestsWithoutResponse());
		dtrs.add(this);
		return dtrs;
	}
}
