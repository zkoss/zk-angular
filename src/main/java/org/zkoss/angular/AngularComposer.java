/** AngularComposer.java.

	Purpose:
		
	Description:
		
	History:
		2:42:27 PM Jul 17, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
 */
package org.zkoss.angular;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindComposer;
import org.zkoss.bind.BindContext;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.Binder;
import org.zkoss.bind.impl.BindContextImpl;
import org.zkoss.bind.impl.BinderUtil;
import org.zkoss.bind.sys.BindEvaluatorX;
import org.zkoss.json.JSONAware;
import org.zkoss.lang.Objects;
import org.zkoss.xel.ExpressionX;
import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.AuService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;

/**
 * An angular JS composer for binding the data from client to server and vice
 * versa.
 * 
 * @author jumperchen
 */
public class AngularComposer<T extends Component> extends BindComposer<T>
		implements AuService {
	private static final Logger log = LoggerFactory
			.getLogger(AngularComposer.class);

	private boolean disableSmartUpdate = false;
	protected Component self;
	private Map<String, Object> _bindingAttrs;

	public void doAfterCompose(T comp) throws Exception {
		self = comp;
		_bindingAttrs = new HashMap<String, Object>(5) {
			public Object put(final String key, Object value) {
				Object oldValue = super.put(key, value);
				if (!disableSmartUpdate) {
					if (value instanceof Collection || value instanceof Object[] || !Objects.equals(oldValue, value)) {
						AngularBinder ngBinder = (AngularBinder) BinderUtil.getBinder(self);
						Clients.response(new AngularInvoke(self, "@load", key,
								ngBinder.getJSONConverter().coerceToUi(value, self, null))  {
							public String getOverrideKey() {
								return "zkng@load" + key;
							}
						});
					}
				}
				return oldValue;
			}
		};
		super.doAfterCompose(comp);

		// bind self au service
		comp.setAuService(this);
	}

	public Map<String, Object> getBindingAttributes() {
		return _bindingAttrs;
	}

	public boolean service(AuRequest request, boolean everError) {
		final String cmd = request.getCommand();
		if ("onBindCommand".equals(cmd)) {
			Map<String, Object> data = request.getData();
			String vcmd = data.get("cmd").toString();

			Binder binder = BinderUtil.getBinder(self);
			binder.postCommand(vcmd, (Map<String, Object>) data.get("args"));
			return true;
		} else if ("onBindGlobalCommand".equals(cmd)) {
			Map<String, Object> data = request.getData();
			String vcmd = data.get("cmd").toString();

			AngularBinder binder = (AngularBinder)BinderUtil.getBinder(self);
			
			BindUtils.postGlobalCommand(binder.getQueueName(), binder.getQueueScope(), vcmd, (Map<String, Object>) data.get("args"));
			return true;
		} else if ("onBindChange".equals(cmd)) {
			Object value = request.getData().get("value");
			String attnm = (String) request.getData().get("attnm");
			String key = (String) request.getData().get("key");
			disableSmartUpdate = true;
			try {
				if (value instanceof JSONAware) {
					AngularBinder binder = (AngularBinder)BinderUtil.getBinder(self);
					BindEvaluatorX evaluatorX = binder.getEvaluatorX();
					BindContext ctx = new BindContextImpl(null, null, false, null,
							self, null);
					ExpressionX parseExpressionX = evaluatorX.parseExpressionX(ctx, attnm, Object.class);
					Class<?> returnType = evaluatorX.getType(ctx, self, parseExpressionX);
					try {
						ctx.setAttribute("org.zkoss.angular.AngularParamCall.type", returnType);
						value = binder.getJSONConverter().coerceToBean(value, self, ctx);	
					} finally {
						ctx.setAttribute("org.zkoss.angular.AngularParamCall.type", null);
					}
				}
				_bindingAttrs.put(key, value);
			}  finally {
				disableSmartUpdate = false;
			}
			Events.postEvent(new Event("onBindChange-" + attnm, self, value));
			return true;
		} else {
			return false;
		}
	}
}
