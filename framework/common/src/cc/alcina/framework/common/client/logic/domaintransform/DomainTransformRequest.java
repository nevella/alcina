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
import java.util.List;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.util.SimpleStringParser;


@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */

 public class DomainTransformRequest implements Serializable {
	private List<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>();

	public void setItems(List<DomainTransformEvent> items) {
		this.items = items;
	}

	private List<DomainTransformRequest> priorRequestsWithoutResponse = new ArrayList<DomainTransformRequest>();

	private int requestId;

	private ClientInstance clientInstance;

	private DomainTransformRequestType domainTransformRequestType;

	@Transient
	public ClientInstance getClientInstance() {
		return clientInstance;
	}

	@Transient
	public List<DomainTransformEvent> getItems() {
		return items;
	}

	/**
	 * Persistence defined in app subclass
	 */
	@Transient
	public List<DomainTransformRequest> getPriorRequestsWithoutResponse() {
		return this.priorRequestsWithoutResponse;
	}

	public int getRequestId() {
		return requestId;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public void setRequestId(int eventId) {
		this.requestId = eventId;
	}

	public String toString() {
		StringBuffer sb2 = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();
		int i = 0;
		for (DomainTransformEvent dte : items) {
			if (++i % 200 == 0) {
				sb2.append(sb1.toString());
				sb1 = new StringBuffer();
			}
			dte.appendTo(sb1);
		}
		sb2.append(sb1.toString());
		return sb2.toString();
	}

	public String toStringForError() {
		List<DomainTransformRequest> dtrs = new ArrayList<DomainTransformRequest>();
		dtrs.addAll(getPriorRequestsWithoutResponse());
		dtrs.add(this);
		StringBuffer sb = new StringBuffer();
		for (DomainTransformRequest dtr : dtrs) {
			sb.append("----");
			sb.append(dtr.getRequestId());
			List<DomainTransformEvent> items = dtr.getItems();
			for (DomainTransformEvent dte : items) {
				String s2 = "\t" + dte.toString().replace("\n", "\n\t") + "\n";
				sb.append(s2);
			}
		}
		return sb.toString();
	}

	public void fromString(String eventsStr) {
		SimpleStringParser p = new SimpleStringParser(eventsStr);
		String s;
		while ((s = p.read(DomainTransformEvent.DATA_TRANSFORM_EVENT_MARKER,
				DomainTransformEvent.DATA_TRANSFORM_EVENT_MARKER, true, false)) != null) {
			items.add(DomainTransformEvent.fromString(s));
		}
	}

	public void setDomainTransformRequestType(
			DomainTransformRequestType domainTransformRequestType) {
		this.domainTransformRequestType = domainTransformRequestType;
	}

	public DomainTransformRequestType getDomainTransformRequestType() {
		return domainTransformRequestType;
	}

	public enum DomainTransformRequestType {
		TO_REMOTE, CLIENT_OBJECT_LOAD, CLIENT_SYNC,TO_REMOTE_COMPLETED,CLIENT_STARTUP_FROM_OFFLINE
	}
}
