package cc.alcina.extras.dev.console.remote.protocol;

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.reflection.Bean;

@Bean
public class RemoteConsoleConsoleChanges extends BaseBindable {
	private boolean clearOutput;

	private String outputHtml;

	private String commandLine;

	public String getCommandLine() {
		return this.commandLine;
	}

	public String getOutputHtml() {
		return this.outputHtml;
	}

	public boolean isClearOutput() {
		return this.clearOutput;
	}

	public void setClearOutput(boolean clearOutput) {
		this.clearOutput = clearOutput;
	}

	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
	}

	public void setOutputHtml(String outputHtml) {
		this.outputHtml = outputHtml;
	}
}
