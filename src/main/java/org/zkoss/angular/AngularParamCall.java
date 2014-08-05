/** AngularParamCall.java.

	Purpose:
		
	Description:
		
	History:
		2:29:39 PM Jul 21, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.angular;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindContext;
import org.zkoss.bind.Converter;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.impl.ParamCall;
import org.zkoss.json.JSONAware;
import org.zkoss.lang.Classes;

/**
 * A smart converter for Angular ParamCall
 * @author jumperchen
 *
 */
public class AngularParamCall extends ParamCall {
	private static final Logger _log = LoggerFactory.getLogger(AngularParamCall.class);
	
	public void setBindingArgs(final Map<String, Object> bindingArgs){
		_paramResolvers.put(BindingParam.class, new ParamResolver<Annotation>() {
			public Object resolveParameter(Annotation anno,Class<?> returnType) {
				Object val = bindingArgs.get(((BindingParam) anno).value());
				if (val == null) return null;
				if (val instanceof JSONAware) {
					BindContext bindContext = getBindContext();
					AngularBinder binder = (AngularBinder) getBinder();
					Converter converter = binder.getJSONConverter();
					if (converter != null) {
						try {
							bindContext.setAttribute("org.zkoss.angular.AngularParamCall.type", returnType);
							Object result = converter.coerceToBean(val, binder.getView(), bindContext);
							return result;
						} finally {
							bindContext.setAttribute("org.zkoss.angular.AngularParamCall.type", null);
						}
					} else 
						return Classes.coerce(returnType, val);
				} else {
					return val==null?null:Classes.coerce(returnType, val);
				}
			}
		});
	}
}
