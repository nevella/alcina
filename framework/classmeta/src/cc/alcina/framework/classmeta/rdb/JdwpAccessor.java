package cc.alcina.framework.classmeta.rdb;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class JdwpAccessor {
    Map<CommandIds, JdwpMessage> messages = new LinkedHashMap<>();

    static class CommandIds {
        private int commandSet;

        private int commandId;

        public CommandIds(int commandSet, int commandId) {
            this.commandSet = commandSet;
            this.commandId = commandId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CommandIds) {
                CommandIds o = (CommandIds) obj;
                return CommonUtils.equals(commandSet, o.commandSet, commandId,
                        o.commandId);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(commandSet, commandId);
        }
        @Override
        public String toString() {
            return Ax.format("%s/%s", commandSet,commandId);
        }
    }

    public JdwpAccessor() {
        try {
            model();
            int debug = 3;
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private void model() throws Exception {
        Class jdwp = Class.forName("com.sun.tools.jdi.JDWP");
        for (Class commandSet : jdwp.getDeclaredClasses()) {
            for (Class command : commandSet.getDeclaredClasses()) {
                JdwpMessage message = new JdwpMessage(commandSet, command);
                CommandIds ids = new CommandIds(message.getCommandSetId(),
                        message.getCommandId());
                messages.put(ids, message);
            }
        }
    }

    public void parse(JdwpPacket packet) {
        CommandIds ids = new CommandIds(packet.commandSet(),
                packet.commandId());
        if(!packet.fromDebugger){
            packet.messageName="(reply)";
            return;
        }
        JdwpMessage message = messages.get(ids);
        packet.messageName = message.
                name();
        packet.message = message;
    }
}
