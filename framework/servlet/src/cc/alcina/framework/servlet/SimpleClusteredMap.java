package cc.alcina.framework.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;

public class SimpleClusteredMap<K, V> {
	public Map<K, V> data;

	List<Instruction> instructions = new ArrayList<Instruction>();

	public SimpleClusteredMap(Map<K, V> data) {
		this.data = data;
	}

	public void clearInstructions() {
		instructions.clear();
	}

	public boolean delete(K key) {
		if (data.containsKey(key)) {
			addAndPlay(new Instruction(null, key, InstructionType.DELETE));
			return true;
		}
		return false;
	}

	public boolean put(K key, V value) {
		if (CommonUtils.equalsWithNullEquality(data.get(key), value)) {
			return false;
		}
		addAndPlay(new Instruction(value, key, InstructionType.PUT));
		return true;
	}

	public void replayInstructions() {
		for (Instruction instruction : instructions) {
			instruction.play();
		}
	}

	private synchronized void addAndPlay(Instruction instruction) {
		instructions.add(instruction);
		instruction.play();
	}

	class Instruction {
		V value;

		K key;

		InstructionType type;

		public Instruction(V value, K key, InstructionType type) {
			this.value = value;
			this.key = key;
			this.type = type;
		}

		@Override
		public String toString() {
			return String.format("Instruction: %s %s\n", type, key);
		}

		void play() {
			switch (type) {
			case DELETE:
				data.remove(key);
				break;
			case PUT:
				data.put(key, value);
				break;
			}
		}
	}

	static enum InstructionType {
		PUT, DELETE
	}
}
