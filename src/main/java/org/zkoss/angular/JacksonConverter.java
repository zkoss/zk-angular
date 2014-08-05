/** JacksonConverter.java.

	Purpose:
		
	Description:
		
	History:
		3:16:11 PM Jul 21, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.angular;

import java.io.IOException;
import java.lang.reflect.Type;

import org.zkoss.bind.BindContext;
import org.zkoss.bind.Converter;
import org.zkoss.zk.ui.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A Jackson converter implementation
 * @author jumperchen
 *
 */
public class JacksonConverter implements Converter<Object, Object, Component> {
	private ObjectMapper jackson = new ObjectMapper();
	public Object coerceToUi(Object beanProp, Component component,
			BindContext ctx) {
		try {
			return jackson.writeValueAsString(beanProp);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private JavaType getType(Type type) {
		return jackson.getTypeFactory().constructType(type, (Class)null);
	}
	public Object coerceToBean(Object compAttr, Component component,
			BindContext ctx) {
		Class<?> type = (Class<?>) ctx.getAttribute("org.zkoss.angular.AngularParamCall.type");
		JavaType javaType = getType(type);
		if (compAttr != null) {
			try {
				return jackson.readValue(compAttr.toString(), javaType);
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}