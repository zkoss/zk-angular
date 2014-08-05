/** GsonConverter.java.

	Purpose:
		
	Description:
		
	History:
		2:39:57 PM Jul 21, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.angular;

import java.lang.reflect.Type;

import org.zkoss.bind.BindContext;
import org.zkoss.bind.Converter;
import org.zkoss.zk.ui.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * A Gson converter implementation
 * @author jumperchen
 */
public class GsonConverter implements Converter<Object, Object, Component> {
	private Gson gson = new Gson();
	public Object coerceToUi(Object beanProp, Component component,
			BindContext ctx) {
		return gson.toJson(beanProp);
	}

	private TypeToken<?> getTypeToken(Type type) {
		return TypeToken.get(type);
	}
	public Object coerceToBean(Object compAttr, Component component,
			BindContext ctx) {
		Class<?> type = (Class<?>) ctx.getAttribute("org.zkoss.angular.AngularParamCall.type");
		TypeToken<?> typeToken = getTypeToken(type);
		if (compAttr != null)
			return gson.fromJson(compAttr.toString(), typeToken.getType());
		else
			return null;
	}

}
