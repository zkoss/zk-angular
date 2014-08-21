/** AngularParser.java.

	Purpose:
		
	Description:
		
	History:
		3:04:49 PM Jul 18, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.angular;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.idom.Attribute;
import org.zkoss.idom.Item;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.metainfo.NamespaceParser;
import org.zkoss.zk.ui.metainfo.NativeInfo;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.metainfo.impl.AnnotationHelper;

/**
 * An angular parser to handle the namespace <code>angular</code> or <code>http://angularjs.org</code>
 * @author jumperchen
 *
 */
public class AngularParser implements NamespaceParser {
	private static final Logger log = LoggerFactory.getLogger(AngularParser.class);
	
	private AnnotationHelper _helper;
	
	protected final static String ANNO_ATTR_FORMAT = "$composer.bindingAttributes['%s']";
	
	private static Map<String, Boolean> RESERVED_WORD = new HashMap<String, Boolean>();
	
	static {
		RESERVED_WORD.put("app", Boolean.TRUE);
		RESERVED_WORD.put("href", Boolean.TRUE);
		RESERVED_WORD.put("src", Boolean.TRUE);
		RESERVED_WORD.put("srcset", Boolean.TRUE);
		RESERVED_WORD.put("disabled", Boolean.TRUE);
		RESERVED_WORD.put("checked", Boolean.TRUE);
		RESERVED_WORD.put("readonly", Boolean.TRUE);
		RESERVED_WORD.put("selected", Boolean.TRUE);
		RESERVED_WORD.put("form", Boolean.TRUE);
		RESERVED_WORD.put("change", Boolean.TRUE);
		RESERVED_WORD.put("list", Boolean.TRUE);
		RESERVED_WORD.put("value", Boolean.TRUE);
		RESERVED_WORD.put("model-options", Boolean.TRUE);
		RESERVED_WORD.put("bind-template", Boolean.TRUE);
//		RESERVED_WORD.put("bind-html", Boolean.TRUE);
		RESERVED_WORD.put("class", Boolean.TRUE);
		RESERVED_WORD.put("class-odd", Boolean.TRUE);
		RESERVED_WORD.put("class-even", Boolean.TRUE);
		RESERVED_WORD.put("cloak", Boolean.TRUE);
		RESERVED_WORD.put("controller", Boolean.TRUE);
		RESERVED_WORD.put("csp", Boolean.TRUE);
		RESERVED_WORD.put("click", Boolean.TRUE);
		RESERVED_WORD.put("dblclick", Boolean.TRUE);
		RESERVED_WORD.put("mousedown", Boolean.TRUE);
		RESERVED_WORD.put("mouseup", Boolean.TRUE);
		RESERVED_WORD.put("mouseover", Boolean.TRUE);
		RESERVED_WORD.put("mouseenter", Boolean.TRUE);
		RESERVED_WORD.put("mouseleave", Boolean.TRUE);
		RESERVED_WORD.put("mousemove", Boolean.TRUE);
		RESERVED_WORD.put("keydown", Boolean.TRUE);
		RESERVED_WORD.put("keyup", Boolean.TRUE);
		RESERVED_WORD.put("keypress", Boolean.TRUE);
		RESERVED_WORD.put("submit", Boolean.TRUE);
		RESERVED_WORD.put("focus", Boolean.TRUE);
		RESERVED_WORD.put("blur", Boolean.TRUE);
		RESERVED_WORD.put("copy", Boolean.TRUE);
		RESERVED_WORD.put("cut", Boolean.TRUE);
		RESERVED_WORD.put("paste", Boolean.TRUE);
		RESERVED_WORD.put("if", Boolean.TRUE);
		RESERVED_WORD.put("include", Boolean.TRUE);
		RESERVED_WORD.put("init", Boolean.TRUE);
		RESERVED_WORD.put("non-bindable", Boolean.TRUE);
		RESERVED_WORD.put("pluralize", Boolean.TRUE);
		RESERVED_WORD.put("repeat", Boolean.TRUE);
		RESERVED_WORD.put("show", Boolean.TRUE);
		RESERVED_WORD.put("hide", Boolean.TRUE);
		RESERVED_WORD.put("style", Boolean.TRUE);
		RESERVED_WORD.put("switch", Boolean.TRUE);
		RESERVED_WORD.put("transclude", Boolean.TRUE);
	}
	public AngularParser() {
		_helper = new AnnotationHelper();
	}
	
	/* (non-Javadoc)
	 * @see org.zkoss.zk.ui.metainfo.NamespaceParser#isMatched(java.lang.String)
	 */
	public boolean isMatched(String nsURI) {
		return "angular".equals(nsURI) || "http://angularjs.org".equals(nsURI);
	}

	private static org.zkoss.util.resource.Location location(Item el) {
		return org.zkoss.xml.Locators.toLocation(el != null ? el.getLocator(): null);
	}
	/* (non-Javadoc)
	 * @see org.zkoss.zk.ui.metainfo.NamespaceParser#parse(org.zkoss.idom.Attribute, org.zkoss.zk.ui.metainfo.ComponentInfo, org.zkoss.zk.ui.metainfo.PageDefinition)
	 */
	public boolean parse(Attribute attr, ComponentInfo compInfo,
			PageDefinition pgdef) throws Exception {
		String name = attr.getLocalName();
		String value = attr.getValue();
		if (AnnotationHelper.isAnnotation(value) && !value.startsWith("@command(") && !value.startsWith("@global-command(")) {
			if (!isAnnotatable(name))
				throw new IllegalArgumentException("The name is reserved: [ng-" + name + "]");
			if (isAnnotationSugar(name)) {
				String newName = RandomStringUtils.randomAlphabetic(6);
				if (compInfo instanceof NativeInfo) {
					compInfo.addProperty("ng-" + name, newName, null);
				} else {
					compInfo.addWidgetAttribute("ng-" + name, newName, null);
				}
				name = newName;
			}
			name = String.format(ANNO_ATTR_FORMAT, name);
			_helper.addByCompoundValue(value.trim(), location(attr));
			_helper.applyAnnotations(compInfo, name, true);
		} else if (compInfo instanceof NativeInfo) {
			compInfo.addProperty("ng-" + name, value, null);
		} else {
			compInfo.addWidgetAttribute("ng-" + name, value, null);
		}
		if (log.isDebugEnabled())
			log.debug("name: {}, value: {}", attr.getLocalName(), attr.getValue());
		return true;
	}

	private static boolean isAnnotationSugar(String attrName) {
		return ("model".equals(attrName) || "bind".equals(attrName) || "bind-html".equals(attrName));
	}
	private static boolean isAnnotatable(String attrName) {
		return !RESERVED_WORD.containsKey(attrName);
	}
	/**
	 * Returns 0 by default.
	 */
	public int getPriority() {
		return 0;
	}

}
