package cc.alcina.framework.common.client.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.ReplayInstruction.ReplayLocator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface IUserStory<U extends IUserStory> extends HasIdAndLocalId {
	String getCart();

	long getClientInstanceId();

	String getClientInstanceUid();

	Date getDate();

	String getEmail();

	String getHttpReferrer();

	@Override
	long getId();

	String getIid();

	String getLocation();

	List<ClientLogRecord> getLogs();

	String getStory();

	String getTrigger();

	String getUserAgent();

	void setCart(String cart);

	void setClientInstanceId(long clientInstanceId);

	void setClientInstanceUid(String clientInstanceUid);

	void setDate(Date date);

	void setEmail(String email);

	void setHttpReferrer(String httpReferrer);

	@Override
	void setId(long id);

	void setIid(String iid);

	void setLocation(String location);

	void setLogs(List<ClientLogRecord> logs);

	void setStory(String story);

	void setTrigger(String trigger);

	void setUserAgent(String userAgent);

	default void obfuscateClientEvents() {
		List<String> buffer = new ArrayList<>();
		for (String line : getStory().split("\n")) {
			ClientLogRecords records = AlcinaBeanSerializer
					.deserializeHolder(line);
			List<ClientLogRecord> list = records.getLogRecords();
			for (ClientLogRecord record : list) {
				switch (record.getTopic()) {
				case "change":
				case "blur":
				case "focus":
					String message = record.getMessage();
					ReplayInstruction instruction = ReplayInstruction
							.fromClientLogRecord(record);
					ReplayLocator replayLocator = ReplayInstruction
							.parseReplayBody(instruction.param1);
					String obfuscatedValue = instruction.param2;
					if(Ax.isBlank(obfuscatedValue)){
						continue;
					}
					if (Ax.notBlank(obfuscatedValue)) {
						if (obfuscatedValue.length() > 4) {
							obfuscatedValue = obfuscatedValue.substring(0, 4)
									+ CommonUtils.padStringLeft("",
											obfuscatedValue.length() - 4, "X");
						} else {
							obfuscatedValue = CommonUtils.padStringLeft("",
									obfuscatedValue.length(), "X");
						}
					}
					String valueMessage = Ax.format("%s%s",
							ClientLogRecord.VALUE_SEPARATOR, obfuscatedValue);
					String replayBody = ReplayInstruction.createReplayBody(
							replayLocator.cssSelector, replayLocator.text,
							valueMessage);
					record.setMessage(replayBody);
					break;
				}
			}
			buffer.add(AlcinaBeanSerializer.serializeHolder(records));
		}
		setStory(buffer.stream().collect(Collectors.joining("\n")));
	}
}