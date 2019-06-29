package cc.alcina.extras.dev.console;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HtmlParser;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class DevConsoleDebugCommands2 {
    public static final String LOCAL_DOM_EXCEPTION_LOG_PATH = "localdom-exception.txt";

    public static final String LOCAL_DOM_EXCEPTION_REMOTE_HTML_PATH = "localdom-remote.html";

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
            Ax.out(model.localHtml);
            Ax.out("\n\n************************\n\n");
            Ax.out(model.diffCalc);
            String remoteOutPath = Ax.format("/tmp/log/%s",
                    LOCAL_DOM_EXCEPTION_REMOTE_HTML_PATH);
            ResourceUtilities.write(model.remoteHtml, remoteOutPath);
            Ax.out("Wrote file to:\n\t%s", remoteOutPath);
            return "OK";
        }

        static class Model {
            String localHtml;

            String remoteHtml;

            private Element remoteParseByLocalDom;

            private Element localParseByLocalDom;

            private String diffCalc;

            public void parse(String log) {
                Pattern p1 = Pattern.compile(
                        "(?s)Text node reparse - remote:\n(.+?</body>)\n\nlocal:\n(.+?</body>)\n");
                Matcher m1 = p1.matcher(log);
                if (m1.find()) {
                    remoteHtml = m1.group(1);
                    localHtml = m1.group(2);
                    remoteParseByLocalDom = Document.get().createElement("div");
                    HtmlParser.debugCursor = false;
                    new HtmlParser().parse(remoteHtml, remoteParseByLocalDom,
                            true);
                    localParseByLocalDom = Document.get().createElement("div");
                    // HtmlParser.debugCursor = true;
                    new HtmlParser().parse(remoteHtml, localParseByLocalDom,
                            true);
                    List<String> remoteLines = Arrays.asList(
                            remoteParseByLocalDom.debugLocalDom().split("\n"));
                    List<String> localLines = Arrays.asList(
                            localParseByLocalDom.debugLocalDom().split("\n"));
                    StringBuilder diffCalcBuilder = new StringBuilder();
                    for (int idx = 0; idx < remoteLines.size()
                            || idx < localLines.size(); idx++) {
                        String l1 = idx < remoteLines.size()
                                ? remoteLines.get(idx)
                                : "---";
                        String l2 = idx < remoteLines.size()
                                ? remoteLines.get(idx)
                                : "---";
                        String out = String.format("%-40s\t%-40s",
                                CommonUtils.trimToWsChars(l1, 40),
                                CommonUtils.trimToWsChars(l2, 40));
                        diffCalcBuilder.append(out);
                        diffCalcBuilder.append("\n");
                    }
                    this.diffCalc = diffCalcBuilder.toString();
                } else {
                    Preconditions.checkState(false);
                }
            }
        }
    }
}
