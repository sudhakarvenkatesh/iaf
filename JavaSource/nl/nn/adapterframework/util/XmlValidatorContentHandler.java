/*
 * $Log: XmlValidatorContentHandler.java,v $
 * Revision 1.3  2012-09-28 13:51:49  m00f069
 * Restored illegalRoot forward and XML_VALIDATOR_ILLEGAL_ROOT_MONITOR_EVENT with new check on root implementation.
 *
 * Revision 1.2  2012/09/19 21:40:37  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ignoreUnknownNamespaces attribute
 *
 * Revision 1.1  2012/09/19 09:49:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 */
package nl.nn.adapterframework.util;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.xni.grammars.Grammar;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import java.util.*;

/**
 * SAX ContentHandler used during XML validation for some additional validation
 * checks and getting more information in case validation fails.
 *
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 */
public class XmlValidatorContentHandler extends DefaultHandler2 {

	// state
	public String elementName = null;
	private int level = -1;
	private List<String> elements = new ArrayList<String>();

	private final Map<String, Grammar> grammarsValidation;
	private final Set<List<String>> rootValidations;
	private final boolean ignoreUnknownNamespaces;

	/**
	 * 
	 * @param grammarsValidation
	 * @param rootValidations
	 *         contains path's (just a single element in case of the root of the
	 *         entire xml) to root elements which should be checked upon
	 * @param ignoreUnknownNamespaces
	 */
	public XmlValidatorContentHandler(Map<String, Grammar> grammarsValidation,
				Set<List<String>> rootValidations,
				boolean ignoreUnknownNamespaces) {
		this.grammarsValidation = grammarsValidation;
		this.rootValidations = rootValidations;
		this.ignoreUnknownNamespaces = ignoreUnknownNamespaces;
	}

	@Override
	public void startElement(String namespaceURI, String lName, String qName,
			Attributes attrs) throws SAXException {
		if (rootValidations != null) {
			for (List<String> path: rootValidations) {
				int i = elements.size();
				if (path.size() == i + 1
						&& elements.equals(path.subList(0, i))
						&& !path.get(i).equals(lName)) {
					throw new IllegalRootElementException(
							"Illegal root element '" + lName + "' at '"
							+ getXpath() + "', expecting element '"
							+ path.get(i) + "'.");
				}
			}
		}
		level++;
		elements.add(lName);
		elementName = lName;
		if (StringUtils.isNotEmpty(namespaceURI)) {
			checkNamespaceExistance(namespaceURI);
		}
	}

	@Override
	public void endElement(String namespaceURI, String lName, String qName)
			throws SAXException {
		elements.remove(level);
		level--;
	}

	@Override
	public void startPrefixMapping(String prefix, String namespace) throws SAXException {
		// nop
	}

	protected void checkNamespaceExistance(String namespace) throws UnknownNamespaceException {
		if (grammarsValidation != null) {
			Grammar grammar = grammarsValidation.get(namespace);
			if (grammar == null && !ignoreUnknownNamespaces) {
				throw new UnknownNamespaceException("Unknown namespace " + namespace);
			}
		}
	}

	public String getElementName() {
		return elementName;
	}

	public String getXpath() {
		StringBuilder xpath = new StringBuilder("/");
		Iterator<String> it = elements.iterator();
		if (it.hasNext()) {
			xpath.append(it.next());
		}
		while (it.hasNext()) {
			xpath.append('/').append(it.next());
		}
		return xpath.toString();
	}

	public static class IllegalRootElementException extends SAXException {
		private static final long serialVersionUID = 1L;

		public IllegalRootElementException(String s) {
			super(s);
		}
	}

	public static class UnknownNamespaceException extends SAXException {
		private static final long serialVersionUID = 1L;

		public UnknownNamespaceException(String s) {
			super(s);
		}
	}

}