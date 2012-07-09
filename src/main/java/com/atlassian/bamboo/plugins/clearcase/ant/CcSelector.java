/**
 * 
 */
package com.atlassian.bamboo.plugins.clearcase.ant;

import java.io.Serializable;

import com.atlassian.bamboo.plugins.clearcase.utils.ValidationException;


/**
 * Holder for ClearCase Sleector Object that can be broker into its parts. It
 * also supports validation of a selector.
 */
public class CcSelector implements Serializable {
	
	private static final long serialVersionUID = -4260329473323726126L;
	//constants for selector Kinds (not necesarily definative bu most
	public static final String  KIND_PROJECT  = "project";
	public static final String  KIND_VOB      = "vob";
	public static final String  KIND_STREAM   = "stream";
	public static final String  KIND_BASELINE = "baseline";
	public static final String  KIND_ATTR     = "attype";
	public static final String  KIND_ACTIVITY = "activity";

	
	public static final String MSG_NULL_SELECTOR = "The selector can not set to null";

	private String kind;

	private String typeName = "";

	private String vob = null;

	private static final String seperator = System
			.getProperty("file.separator");

	/**
	 * Specifiy on the Selector kind.
	 */
	public CcSelector(String kind) {
		this.kind = kind;
	}

	/**
	 * Construct an initialise the project.
	 * 
	 * @param selector
	 *            the project selector string.
	 * @throws ValidationException if the selector passed is not valid
	 */
	public CcSelector(String kind, String selector) throws ValidationException {
		this(kind);
		setAsString(selector);
	}

	/**
	 * Specify the type of seletecto this represents.
	 * 
	 * @param kind
	 *            the kind
	 */
	public void setKind(String kind) {
		this.kind = kind;
	}

	/**
	 * Get kind of selector this object represents, can be null or empty string.
	 * 
	 * @return the kind.
	 */
	public String getKind() {
		return this.kind;
	}

	/**
	 * Parses the ClearCase project selector into its parts. this methos is
	 * lenited in that it will work with just aproject name or a fully qualified
	 * selector, examples of acceptable selectors are:
	 * 
	 * <pre>
	 *    Proj1
	 *    project:Proj1
	 *    Proj1@\Pvob
	 *    project:Proj1@\Pvob
	 * </pre>
	 * 
	 * @param selector
	 *            the project selector string.
	 * @throws ValidationException if the selector passed is not valid
	 */
	public void setAsString(String selector) throws ValidationException {
		if(selector == null)
		{
			throw new ValidationException(MSG_NULL_SELECTOR);
		}
		int start = selector.indexOf(':');
		if (start >= 0) {
			String statedKind = selector.substring(0, start);
			if (!isValidKind(statedKind)) {
				throw new ValidationException("Selector kind ["+kind+"] did not match that passed. value["+selector+"]");
			}
			setKindIfNull(statedKind);
			selector = selector.substring(start + 1);
		}
		vob = null;
		start = selector.indexOf('@');
		if (start >= 0) {
			typeName = selector.substring(0, start);
			if (selector.length() > start + 1) {
				vob = selector.substring(start + 2);
			}
			while (vob != null && vob.startsWith(seperator)) {
				vob = vob.substring(seperator.length());
			}
		} else {
			typeName = selector;
		}
	}

	/**
	 * If the current kind is empty then update the kind to the value passed.
	 * 
	 * @param statedKind
	 *            the kind to set if the kind is empty.
	 */
	private void setKindIfNull(String statedKind) {
		if (isEmptyString(kind)) {
			this.kind = statedKind;
		}
	}

	/**
	 * Check if the supplied kind matches this selector. the anwser will always
	 * be true if the kind has not been specified (ie null or empty string.
	 * 
	 * @param statedKind
	 *            kind to compare against this selector type.
	 * @return true if matches false if different.
	 */
	public boolean isValidKind(String statedKind) {
		boolean rval = true;
		if (!isEmptyString(kind)) {
			rval = kind.equals(statedKind);
		}
		return rval;
	}

	/**
	 * The name portion of the typed selector.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return typeName;
	}

	/**
	 * Set the name portion of the selector.
	 * 
	 * @param name the name to set.
	 */
	public void setName(String name)
	{
		this.typeName = name;
	}
	
	/**
	 * The vob selector string only no loadning seperator.
	 * 
	 * @return
	 */
	public String getVobSelector() {
		return vob;
	}

	/**
	 * Thhe project as fully qualified clearcase selectopr string.
	 * 
	 * @return
	 */
	public String asSelector() {
		StringBuilder rval = new StringBuilder();
		if (!isEmptyString(kind)) {
			rval.append(kind + ":");
		}
		rval.append(typeName);
		if (!isEmptyString(vob)) {
			rval.append("@" + seperator + vob);
		}
		return rval.toString();
	}

	public String toString() {
		return asSelector();
	}

	boolean isEmptyString(String in) {
		return in == null || in.trim().length() == 0;
	}
}
