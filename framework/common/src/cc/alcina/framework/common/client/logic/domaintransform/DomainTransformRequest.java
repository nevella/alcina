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
import java.util.Optional;
import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;

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

	public static DomainTransformRequest createNonServerPersistableRequest() {
		return new DomainTransformRequest();
	}

	public static DomainTransformRequest createPersistableRequest() {
		DomainTransformRequest request = new DomainTransformRequest();
		DomainTransformRequestChunkUuid chunkUuid = new DomainTransformRequestChunkUuid();
		chunkUuid.uuid = createUUID();
		request.setChunkUuidString(chunkUuid.serialize());
		return request;
	}

	public static DomainTransformRequest createSubRequest(
			DomainTransformRequest fullRequest, IntPair range) {
		DomainTransformRequest request = new DomainTransformRequest();
		DomainTransformRequestChunkUuid chunkUuid = DomainTransformRequestChunkUuid
				.deserialize(fullRequest.getChunkUuidString());
		chunkUuid.fromRange(range, fullRequest.allTransforms().size());
		request.setChunkUuidString(chunkUuid.serialize());
		return request;
	}

	public static String createUUID() {
		// http://www.ietf.org/rfc/rfc4122.txt
		char[] chars = new char[36];
		String hexDigits = "0123456789abcdef";
		for (int i = 0; i < 36; i++) {
			chars[i] = hexDigits
					.charAt((int) (Math.floor(Math.random() * 0x10)));
		}
		chars[14] = '4'; // bits 12-15 of the time_hi_and_version field to
							// 0010
		chars[19] = hexDigits.charAt((chars[19] & 0x3) | 0x8); // bits 6-7
																// of
																// the
																// clock_seq_hi_and_reserved
																// to 01
		chars[8] = chars[13] = chars[18] = chars[23] = '-';
		return String.valueOf(chars);
	}

	public static DomainTransformRequest fromString(String eventsStr,
			String chunkUuidString) {
		DomainTransformRequest dtr = new DomainTransformRequest();
		new DTRProtocolSerializer().deserialize(dtr, dtr.getProtocolVersion(),
				eventsStr);
		dtr.setChunkUuidString(chunkUuidString);
		return dtr;
	}

	private String chunkUuidString;

	private List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();

	private List<DomainTransformRequest> priorRequestsWithoutResponse = new ArrayList<DomainTransformRequest>();

	private int requestId;

	private Set<Long> eventIdsToIgnore = new HashSet<Long>();

	private ClientInstance clientInstance;

	private String protocolVersion;

	private String tag;

	@Transient
	public Map<String, String> properties;

	public DomainTransformRequest() {
	}

	/*
	 * These will be in order of application
	 */
	public List<DomainTransformRequest> allRequests() {
		List<DomainTransformRequest> dtrs = new ArrayList<DomainTransformRequest>();
		dtrs.addAll(getPriorRequestsWithoutResponse());
		dtrs.add(this);
		return dtrs;
	}

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
				if (event.getObjectId() != 0) {
					continue;
				}
				if (!createIds.add(event.getObjectLocalId())) {
					itr.remove();
					duplicates = true;
				}
			}
		}
		return duplicates;
	}

	@Transient
	public String getChunkUuidString() {
		return this.chunkUuidString;
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

	public Optional<DomainTransformRequestChunkUuid> provideChunkUuid() {
		return chunkUuidString == null ? Optional.empty()
				: Optional.of(DomainTransformRequestChunkUuid
						.deserialize(chunkUuidString));
	}

	public DomainTransformRequest
			provideRequestForUuidString(String chunkUuidString) {
		return allRequests().stream()
				.filter(r -> r.chunkUuidString.equals(chunkUuidString))
				.findFirst().get();
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

	public void setChunkUuidString(String chunkUuidString) {
		this.chunkUuidString = chunkUuidString;
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

	@Override
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

	public static class DomainTransformRequestChunkUuid {
		public static DomainTransformRequestChunkUuid
				deserialize(String string) {
			if (string == null) {
				return null;
			}
			DomainTransformRequestChunkUuid result = new DomainTransformRequestChunkUuid();
			RegExp regExp = RegExp.compile("(.+?)::(\\d+)::(\\d+)::(\\d+)");
			MatchResult matchResult = regExp.exec(string);
			if (matchResult == null) {
				result.uuid = string;
			} else {
				result.uuid = matchResult.getGroup(1);
				result.firstTransformIndex = Integer
						.parseInt(matchResult.getGroup(2));
				result.lastTransformIndex = Integer
						.parseInt(matchResult.getGroup(3));
				result.totalTransformCount = Integer
						.parseInt(matchResult.getGroup(4));
			}
			return result;
		}

		public String uuid;

		public int firstTransformIndex;

		public int lastTransformIndex;

		public int totalTransformCount;

		public void fromRange(IntPair range, int totalTransformCount) {
			firstTransformIndex = range.i1;
			lastTransformIndex = range.i2;
			this.totalTransformCount = totalTransformCount;
		}

		public String serialize() {
			if (totalTransformCount == 0) {
				return uuid;
			} else {
				return Ax.format("%s::%s::%s::%s", uuid, firstTransformIndex,
						lastTransformIndex, totalTransformCount);
			}
		}
	}
}
