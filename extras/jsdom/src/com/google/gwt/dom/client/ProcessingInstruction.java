/*
 * Copyright 2008 Google Inc.
 *
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
package com.google.gwt.dom.client;

import org.w3c.dom.DOMException;

import cc.alcina.framework.common.client.util.Ax;

/**
 * The ProcessingInstruction interface represents textual content.
 */
public class ProcessingInstruction extends Node implements
		ClientDomProcessingInstruction, org.w3c.dom.ProcessingInstruction {
	/**
	 * Assert that the given {@link Node} is of type
	 * {@link Node#PROCESSING_INSTRUCTION_NODE} and automatically typecast it.
	 */
	public static ProcessingInstruction as(Node node) {
		assert node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE;
		return (ProcessingInstruction) node;
	}

	private ProcessingInstructionLocal local;

	private ClientDomProcessingInstruction remote;

	protected ProcessingInstruction(ProcessingInstructionLocal local) {
		this.local = local;
	}

	@Override
	public ProcessingInstruction cast() {
		return this;
	}

	@Override
	public String getData() {
		return local().getData();
	}

	@Override
	public String getTarget() {
		return local().getTarget();
	}

	@Override
	public String getTextContent() throws DOMException {
		return getData();
	}

	@Override
	public ProcessingInstructionJso jsoRemote() {
		return (ProcessingInstructionJso) remote();
	}

	@Override
	protected ProcessingInstructionLocal local() {
		return local;
	}

	@Override
	public Node node() {
		return this;
	}

	@Override
	protected void putRemote(ClientDomNode remote) {
		this.remote = (ClientDomProcessingInstruction) remote;
	}

	@Override
	protected ClientDomProcessingInstruction remote() {
		return remote;
	}

	@Override
	protected void resetRemote0() {
		this.remote = null;
	}

	@Override
	public void setData(String data) {
		local().setData(data);
		sync(() -> remote().setData(data));
	}

	@Override
	public String toString() {
		return Ax.format("<?%s %s?>", local().getTarget(), local().getData());
	}
}
