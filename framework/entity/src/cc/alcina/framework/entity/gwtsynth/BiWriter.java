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
package cc.alcina.framework.entity.gwtsynth;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

class BiWriter extends PrintWriter {
	private StringWriter stringWriter;

	private PrintWriter printWriter2;

	private PrintWriter printWriter;

	public BiWriter(PrintWriter firstWriter) {
		super(firstWriter);
		this.printWriter = firstWriter;
		this.stringWriter = new StringWriter();
		this.printWriter2 = new PrintWriter(stringWriter);
	}

	public PrintWriter append(char c) {
		this.printWriter2.append(c);
		return this.printWriter.append(c);
	}

	public PrintWriter append(CharSequence csq) {
		this.printWriter2.append(csq);
		return this.printWriter.append(csq);
	}

	public PrintWriter append(CharSequence csq, int start, int end) {
		this.printWriter2.append(csq, start, end);
		return this.printWriter.append(csq, start, end);
	}

	public boolean checkError() {
		this.printWriter2.checkError();
		return this.printWriter.checkError();
	}

	public void close() {
		this.printWriter2.close();
		this.printWriter.close();
	}

	public boolean equals(Object obj) {
		this.printWriter2.equals(obj);
		return this.printWriter.equals(obj);
	}

	public void flush() {
		this.printWriter2.flush();
		this.printWriter.flush();
	}

	public PrintWriter format(Locale l, String format, Object... args) {
		this.printWriter2.format(l, format, args);
		return this.printWriter.format(l, format, args);
	}

	public PrintWriter format(String format, Object... args) {
		this.printWriter2.format(format, args);
		return this.printWriter.format(format, args);
	}

	public PrintWriter getPrintWriter() {
		return this.printWriter;
	}

	public StringWriter getStringWriter() {
		return this.stringWriter;
	}

	public int hashCode() {
		this.printWriter2.hashCode();
		return this.printWriter.hashCode();
	}

	public void print(boolean b) {
		this.printWriter2.print(b);
		this.printWriter.print(b);
	}

	public void print(char c) {
		this.printWriter2.print(c);
		this.printWriter.print(c);
	}

	public void print(char[] s) {
		this.printWriter2.print(s);
		this.printWriter.print(s);
	}

	public void print(double d) {
		this.printWriter2.print(d);
		this.printWriter.print(d);
	}

	public void print(float f) {
		this.printWriter2.print(f);
		this.printWriter.print(f);
	}

	public void print(int i) {
		this.printWriter2.print(i);
		this.printWriter.print(i);
	}

	public void print(long l) {
		this.printWriter2.print(l);
		this.printWriter.print(l);
	}

	public void print(Object obj) {
		this.printWriter2.print(obj);
		this.printWriter.print(obj);
	}

	public void print(String s) {
		this.printWriter2.print(s);
		this.printWriter.print(s);
	}

	public PrintWriter printf(Locale l, String format, Object... args) {
		this.printWriter2.printf(l, format, args);
		return this.printWriter.printf(l, format, args);
	}

	public PrintWriter printf(String format, Object... args) {
		this.printWriter2.printf(format, args);
		return this.printWriter.printf(format, args);
	}

	public void println() {
		this.printWriter2.println();
		this.printWriter.println();
	}

	public void println(boolean x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(char x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(char[] x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(double x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(float x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(int x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(long x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(Object x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void println(String x) {
		this.printWriter2.println(x);
		this.printWriter.println(x);
	}

	public void setPrintWriter(PrintWriter printWriter) {
		this.printWriter = printWriter;
	}

	public void setStringWriter(StringWriter stringWriter) {
		this.stringWriter = stringWriter;
	}

	

	public void write(char[] buf) {
		this.printWriter2.write(buf);
		this.printWriter.write(buf);
	}

	public void write(char[] buf, int off, int len) {
		this.printWriter2.write(buf, off, len);
		this.printWriter.write(buf, off, len);
	}

	public void write(int c) {
		this.printWriter2.write(c);
		this.printWriter.write(c);
	}

	public void write(String s) {
		this.printWriter2.write(s);
		this.printWriter.write(s);
	}

	public void write(String s, int off, int len) {
		this.printWriter2.write(s, off, len);
		this.printWriter.write(s, off, len);
	}
}