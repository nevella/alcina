package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.RdbJdi;
import com.sun.tools.jdi.VirtualMachineImplExt;

import cc.alcina.framework.classmeta.rdb.Packet.EventSeries;
import cc.alcina.framework.classmeta.rdb.Packet.PacketPair;
import cc.alcina.framework.classmeta.rdb.PacketEndpointHost.PacketEndpoint;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

class Oracle {
	Accessor jwdpAccessor = new Accessor();

	VirtualMachineImplExt vm;

	DebuggerState state = new DebuggerState();

	RdbJdi rdbJdi;

	private Endpoint endpoint;

	private InternalVmConnection connection;

	EventSeries predictForSeries = null;

	Set<String> unknownNames = new LinkedHashSet<>();

	public Oracle(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public Packet createAckPacket(Packet packet) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean handlePacket(PacketEndpoint packetSource, Packet packet) {
		if (packet.isReply) {
			Packet reply = packet;
			Packet command = packet.getCorrespondingCommandPacket();
			if (command.fromDebugger) {
				if (endpoint.isDebuggee()) {
					if (command.messageName.equals("FieldsWithGeneric")) {
						int debug = 3;
					}
					predictForSeries = command.series;
					switch (command.messageName) {
					case "ClassesBySignature":
						predict_classes_by_signature(command, reply);
						break;
					case "ReferenceType":
						predict_reference_type(command, reply);
						break;
					}
					switch (command.series) {
					case all_threads_handshake: {
						if (command.messageName.equals("AllThreads")) {
							predict_all_threads_handshake(command, reply);
						}
						break;
					}
					case frames: {
						if (command.messageName.equals("Frames")) {
							predict_frames(command, reply);
						}
						break;
					}
					case variable_table: {
						if (command.messageName
								.equals("VariableTableWithGeneric")) {
							predict_variable_table(command, reply);
						}
						break;
					}
					case get_values_stack_frame: {
						if (command.messageName.equals("GetValues")) {
							predict_get_values_stack_frame(command, reply);
						}
						break;
					}
					case get_values_reference_type: {
						if (command.messageName.equals("GetValues")) {
							predict_get_values_reference_type(command, reply);
						}
						break;
					}
					case get_values_object_reference: {
						if (command.messageName.equals("GetValues")) {
							predict_get_values_object_reference(command, reply);
						}
						break;
					}
					case get_values_array_reference: {
						if (command.messageName.equals("GetValues")) {
							predict_get_values_array_reference(command, reply);
						}
						break;
					}
					}
				}
			}
		} else {
			Packet command = packet;
			if (endpoint.isDebuggee()) {
				if (command.messageName.equals("Composite")) {
					send_composite_reply(command);
				} else if (command.messageName.equals("IsCollected")) {
					debug_is_collected(command);
				} else if (command.messageName.equals("Resume")) {
					return send_resume_command(command);
				}
			} else {
				if (command.notifySuspended) {
					Ax.err("Supsend id: %s = > %s", state.currentSuspendId,
							command.id());
					state.currentSuspendId = command.id();
				}
			}
		}
		return true;
	}

	public boolean isCacheable(Packet packet) {
		if (packet.messageName == null) {
			// we're not trying to prune objects, just get rid of uncacheable
			// values - if the key (command) is gone, this (reply) is
			// unreachable
			return true;
		}
		switch (packet.messageName) {
		// can be forcibly invalidated if need be
		case "Name":
			// valid for vm lifetime
		case "ThreadGroup":
		case "Signature":
		case "SignatureWithGeneric":
		case "ClassesBySignature":
		case "ReferenceType":
		case "MethodsWithGeneric":
		case "LineTable":
		case "SourceDebugExtension":
		case "SourceFile":
		case "FieldsWithGeneric":
		case "Modifiers":
		case "Superclass":
		case "Interfaces":
		case "Parent":
		case "Version":
		case "Capabilities":
		case "CapabilitiesNew":
		case "ClassLoader":
		case "ReflectedType":
		case "VariableTableWithGeneric":
			return true;
		case "Set":
			return false;
		case "GetValues":
			// case "Value":
		case "Frames":
		case "FrameCount":
		case "Length":
		case "CurrentContendedMonitor":
			// valid for the frame lifetime (and the frame is discarded)
		case "ThisObject":
			if (packet.suspendId == state.currentSuspendId) {
				// TODO - what about code execution? (as in the eclipse display
				// view)?
				return true;
			} else {
				return false;
			}
		case "Status":
			/*
			 * bit of a juggle here - we *really* want it for initial load but
			 * really don't want it for thread pause/suspend - thing is, eclipse
			 * checks status every 50ms or so when pausing, so a counter works
			 * OK (boolean doesn't)
			 */
			return packet.predictivePacketUseCount < 3;
		default:
			if (unknownNames.add(packet.messageName)) {
				Ax.err("Invalidating unknown packet :: %s", packet.messageName);
			}
			return false;
		}
	}

	public boolean onPredictivePacketHit(Packet command,
			Packet predictiveResponse) {
		switch (command.messageName) {
		case "Frames":
			int debug = 3;
			break;
		}
		return isCacheable(predictiveResponse);
	}

	public void receivedPredictivePacket() {
		if (endpoint.isDebuggee()) {
			synchronized (connection) {
				connection.notifyAll();
			}
		}
	}

	private void debug_is_collected(Packet command) {
		try {
			rdbJdi.debug_is_collected(command.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_all_threads_handshake(Packet command, Packet reply) {
		try {
			Ax.out(Thread.currentThread() + ":"
					+ Thread.currentThread().getId());
			rdbJdi.predict_all_threads_handshake(command.bytes, reply.bytes);
		} catch (Exception e) {
			Ax.out(Thread.currentThread() + ":"
					+ Thread.currentThread().getId());
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_classes_by_signature(Packet command, Packet reply) {
		try {
			Ax.out(Thread.currentThread() + ":"
					+ Thread.currentThread().getId());
			rdbJdi.predict_classes_by_signature(command.bytes, reply.bytes);
		} catch (Exception e) {
			Ax.out(Thread.currentThread() + ":"
					+ Thread.currentThread().getId());
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_frames(Packet command, Packet reply) {
		try {
			rdbJdi.predict_frames(command.bytes, reply.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_get_values_array_reference(Packet command,
			Packet reply) {
		try {
			rdbJdi.predict_get_values_array_reference(command.bytes,
					reply.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_get_values_object_reference(Packet command,
			Packet reply) {
		try {
			rdbJdi.predict_get_values_object_reference(command.bytes,
					reply.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_get_values_reference_type(Packet command,
			Packet reply) {
		try {
			rdbJdi.predict_get_values_reference_type(command.bytes,
					reply.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_get_values_stack_frame(Packet command, Packet reply) {
		try {
			rdbJdi.predict_get_values_stack_frame(command.bytes, reply.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_reference_type(Packet command, Packet reply) {
		try {
			Ax.out(Thread.currentThread() + ":"
					+ Thread.currentThread().getId());
			rdbJdi.predict_reference_type(command.bytes, reply.bytes);
		} catch (Exception e) {
			Ax.out(Thread.currentThread() + ":"
					+ Thread.currentThread().getId());
			throw new WrappedRuntimeException(e);
		}
	}

	private void predict_variable_table(Packet command, Packet reply) {
		try {
			rdbJdi.predict_variable_table(command.bytes, reply.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void send_composite_reply(Packet command) {
		try {
			boolean notifySuspended = rdbJdi.handle_composite(command.bytes);
			command.notifySuspended = notifySuspended;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private boolean send_resume_command(Packet command) {
		try {
			return rdbJdi.send_resume_command(command.bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	void analysePacket(Packet packet) {
		if (endpoint.isDebugger()
				&& packet.source == endpoint.streams.packetEndpoint()) {
			state.currentPacket = packet;
			state.updateState();
			if (packet.series == EventSeries.unknown) {
				packet.series = state.currentSeries;
			}
		}
	}

	void beforePacketMiss(Packet packet) {
		switch (packet.messageName) {
		case "Suspend":
		case "VariableTableWithGeneric":
		case "FrameCount":
			// cool, not an unforced miss
			return;
		// case "Signature":
		// // not cool but livable
		// return;
		}
		boolean debugHits = false;
		switch (state.currentSeries) {
		case frames:
		case variable_table:
		case get_values_stack_frame:
		case get_values_array_reference:
		case get_values_object_reference:
			break;
		case get_values_reference_type:
			debugHits = true;
			break;
		}
		if (debugHits) {
			Ax.out("\n==============\nDebug packet miss\n============\n");
			Ax.out("Packet dump:\n");
			packet.dump();
			List<Packet> hits = packet.source.otherPacketEndpoint()
					.currentPredictivePacketsHit();
			List<PacketPair> like = packet.source.otherPacketEndpoint()
					.getPredictivePacketsLike(packet);
			List<PacketPair> last = packet.source.otherPacketEndpoint()
					.getMostRecentPredictivePacketList();
			if (like.size() > 0) {
				Ax.out("First like packet dump:\n");
				like.get(0).command.dump();
			}
			Ax.out("***hits***");
			Ax.out(hits);
			Ax.out("\n***like***");
			Ax.out(like);
			Ax.out("\n***last***");
			Ax.out(last);
			Ax.out("");
			if (packet.messageName.equals("ThisObject")) {
				rdbJdi.debugThisObject(packet.bytes);
			}
		}
	}

	void onPredictivePacketMiss() {
		Packet currentPacket = state.currentPacket;
		state.setExpectingPredictiveAfter(null);
		state.updateState();
	}

	void preparePacket(Packet packet) {
		if (!packet.preparedByOracle) {
			packet.preparedByOracle = true;
			packet.mustSend = true;
			jwdpAccessor.parse(packet);
		}
	}

	void start() {
		if (endpoint.isDebuggee()) {
			connection = new InternalVmConnection();
		}
		vm = new VirtualMachineImplExt(connection);
		rdbJdi = new RdbJdi(vm);
	}

	class InternalVmConnection extends Connection {
		@Override
		public void close() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public byte[] readPacket() throws IOException {
			while (true) {
				Packet packet = streams().packetEndpoint()
						.nextIncomingPredictivePacket();
				if (packet != null) {
					packet = streams().translateToOriginalId(packet);
					return packet.bytes;
				}
				synchronized (connection) {
					try {
						connection.wait();
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		}

		@Override
		public void writePacket(byte[] pkt) throws IOException {
			Packet packet = new Packet(null);
			packet.bytes = pkt;
			packet.isPredictive = true;
			packet.predictiveFor = predictForSeries;
			jwdpAccessor.parse(packet);
			streams().write(packet);
		}

		JdwpStreams streams() {
			return endpoint.streams;
		}
	}
}
