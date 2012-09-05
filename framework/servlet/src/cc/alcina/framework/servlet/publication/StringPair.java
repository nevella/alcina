/**
 * 
 */
package cc.alcina.framework.servlet.publication;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
/**
 * A string tuple
 * @author nreddel@barnet.com.au
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class StringPair implements Serializable{
	public String s1;

	public String s2;

	public StringPair() {
	}

	public StringPair(String s1, String s2) {
		this.s1 = s1;
		this.s2 = s2;
	}
}