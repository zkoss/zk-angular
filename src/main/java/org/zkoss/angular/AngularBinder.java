/** AngularBinder.java.

	Purpose:
		
	Description:
		
	History:
		4:08:38 PM Jul 17, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
 */
package org.zkoss.angular;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.AnnotateBinder;
import org.zkoss.bind.BindContext;
import org.zkoss.bind.Converter;
import org.zkoss.bind.impl.BindEvaluatorXImpl;
import org.zkoss.bind.impl.ParamCall;
import org.zkoss.bind.sys.BindEvaluatorX;
import org.zkoss.lang.Strings;
import org.zkoss.xel.ExpressionX;
import org.zkoss.xel.XelException;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.ext.Native;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.util.Clients;

/**
 * An angular JS binder for binding the value from client to server and vise
 * versa.
 * 
 * @author jumperchen
 */
public class AngularBinder extends AnnotateBinder {
	protected static final String JSON_CONVERTER = "angularJsonConverter";
	private static final Logger log = LoggerFactory
			.getLogger(AngularBinder.class);
	private static Pattern EXPR = Pattern
			.compile(AngularParser.ANNO_ATTR_FORMAT.replace("$", "\\$")
					.replace(".", "\\.").replace("[", "\\[")
					.replace("]", "\\]").replace("(", "\\(")
					.replace(")", "\\)").replace("%s", "(.*?)"));

	private String _qname;
	private String _qscope;
	public AngularBinder() {
		this(null, null);
	}

	public AngularBinder(String qname, String qscope) {
		super(qname, qscope);
		_qname = qname != null && !Strings.isEmpty(qname) ? qname : DEFAULT_QUEUE_NAME;
		_qscope = qscope != null && !Strings.isBlank(qscope) ? qscope : DEFAULT_QUEUE_SCOPE;
	}
	
	public String getQueueName() {
		return _qname;
	}
	public String getQueueScope() {
		return _qscope;
	}

	@SuppressWarnings("rawtypes")
	public Converter getJSONConverter() {
		return getConverter(AngularBinder.JSON_CONVERTER);
	}
		

	public void addPropertyInitBinding(Component comp, String attr,
			String initExpr, Map<String, Object> initArgs,
			String converterExpr, Map<String, Object> converterArgs) {
		if (comp instanceof Native)
			comp = this.getView();
		super.addPropertyInitBinding(comp, attr, initExpr, initArgs, converterExpr,
				converterArgs);
	}

	public void addPropertyLoadBindings(Component comp, String attr,
			String loadExpr, String[] beforeCmds, String[] afterCmds,
			Map<String, Object> bindingArgs, String converterExpr,
			Map<String, Object> converterArgs) {
		Matcher matcher = EXPR.matcher(attr);
		if (matcher.find()) {
			final String key = matcher.group(1);
			if (comp instanceof Native)
				comp = this.getView();
			if (log.isDebugEnabled())
				log.debug("LoadBinding, attr {} loadExpr {} ", key, loadExpr);
			Clients.response(new AuInvoke(comp, "@load", key, null) {
				public String getOverrideKey() {
					return "zkng@load" + key;
				}
			});
		}
		super.addPropertyLoadBindings(comp, attr, loadExpr, beforeCmds, afterCmds,
				bindingArgs, converterExpr, converterArgs);
	}

	public void addPropertySaveBindings(Component comp, String attr,
			String saveExpr, String[] beforeCmds, String[] afterCmds,
			Map<String, Object> bindingArgs, String converterExpr,
			Map<String, Object> converterArgs, String validatorExpr,
			Map<String, Object> validatorArgs) {
		Matcher matcher = EXPR.matcher(attr);
		if (matcher.find()) {
			final String key = matcher.group(1);

			if (comp instanceof Native)
				comp = this.getView();
			
			final Map<String, String[]> annotAttrs = new LinkedHashMap<String, String[]>();
			annotAttrs.put("SAVE_EVENT", new String[]{"onBindChange-" + saveExpr});
			annotAttrs.put("ACCESS", new String[]{"save"});
			((ComponentCtrl)comp).addAnnotation(attr, "ZKBIND", annotAttrs);
			if (log.isDebugEnabled())
				log.debug("SaveBinding, attr {} saveExpr {} ", key, saveExpr);
			Clients.response(new AuInvoke(comp, "@save", key, saveExpr) {
				public String getOverrideKey() {
					return "zkng@save" + key;
				}
			});
		}
		super.addPropertySaveBindings(comp, attr, saveExpr, beforeCmds,
				afterCmds, bindingArgs, converterExpr, converterArgs,
				validatorExpr, validatorArgs);
	}

	protected ParamCall createParamCall(BindContext ctx) {
		final ParamCall call = new AngularParamCall();
		call.setBinder(this);
		call.setBindContext(ctx);
		final Component comp = ctx.getComponent();
		if (comp != null) {
			call.setComponent(comp);
		}
		final Execution exec = Executions.getCurrent();
		if (exec != null) {
			call.setExecution(exec);
		}
		return call;
	}
	

	private BindEvaluatorX _eval;
	public BindEvaluatorX getEvaluatorX() {
		if (_eval == null) {
			_eval = new BindEvaluatorXImpl(null, org.zkoss.bind.xel.BindXelFactory.class) {
				public ExpressionX parseExpressionX(BindContext ctx, String expression, Class<?> expectedType)
						throws XelException {
					if (expression.startsWith("self.$composer")) {
						expression = expression.substring(5);
					}
					return super.parseExpressionX(ctx, expression, expectedType);
				}
			};
		}
		return _eval;
	}
}
