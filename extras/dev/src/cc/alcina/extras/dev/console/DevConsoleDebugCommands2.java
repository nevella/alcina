package cc.alcina.extras.dev.console;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;

public class DevConsoleDebugCommands2 {
    public static final String LOCAL_DOM_EXCEPTION_LOG_PATH = "localdom-exception.txt";

    public static class CmdLocalDomDebugTools extends DevConsoleCommand {
        @Override
        public boolean canUseProductionConn() {
            return true;
        }

        @Override
        public String[] getCommandIds() {
            return new String[] { "dxld" };
        }

        @Override
        public String getDescription() {
            return "Work on localdom exception logs";
        }

        @Override
        public String getUsage() {
            return "dxdl {no args - dump logs}";
        }

        @Override
        public boolean rerunIfMostRecentOnRestart() {
            return true;
        }

        @Override
        public String run(String[] argv) throws Exception {
            String log = ResourceUtilities.read(
                    Ax.format("/tmp/log/%s", LOCAL_DOM_EXCEPTION_LOG_PATH));
            Model model = new Model();
            model.parse(log);
            return "OK";
        }

        static class Model {
            String localHtml;

            String remoteHtml;

            public void parse(String log) {
                // TODO Auto-generated method stub
            }
        }
    }
}
