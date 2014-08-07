/** AuInclude.java.

	Purpose:
		
	Description:
		
	History:
		11:13:05 AM Jul 24, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
 */
package org.zkoss.angular;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.zkoss.json.JSONArray;
import org.zkoss.lang.Library;
import org.zkoss.mesg.Messages;
import org.zkoss.web.servlet.Charsets;
import org.zkoss.zk.au.http.AuExtension;
import org.zkoss.zk.au.http.DHtmlUpdateServlet;
import org.zkoss.zk.mesg.MZk;
import org.zkoss.zk.ui.ComponentNotFoundException;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.http.ExecutionImpl;
import org.zkoss.zk.ui.http.WebManager;
import org.zkoss.zk.ui.impl.RequestInfoImpl;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.metainfo.PageDefinitions;
import org.zkoss.zk.ui.sys.RequestInfo;
import org.zkoss.zk.ui.sys.WebAppCtrl;

/**
 * A <tt>zk-ng-include</tt> directive template handler.
 * <p> The default file extension for handling in a dynamic resource with ZK parser is <tt>.zul</tt>,
 * <tt>.xthml</tt>, and <tt>.zhtml</tt>, you can specify your own with the library property
 * <tt>org.zkoss.angular.AnInclude.fileExt</tt>
 * @author jumperchen
 *
 */
public class AuInclude implements AuExtension {
	private ServletContext _ctx;
	private Map<String, Boolean> _fileType;
	public void init(DHtmlUpdateServlet servlet) throws ServletException {
		_ctx = servlet.getServletContext();
		_fileType = new HashMap<String, Boolean>(3);
		for (String k : Library.getProperty("org.zkoss.angular.AuInclude.fileExt", ".zul,.xhtml,.zhtml").split(",")) {
			_fileType.put(k, Boolean.TRUE);
		}
	}

	public static void init(WebApp wapp) throws ServletException {
		if (DHtmlUpdateServlet.getAuExtension(wapp, "/include") == null) {
			DHtmlUpdateServlet
					.addAuExtension(wapp, "/ngInclude", new AuInclude());
		}
	}

	public void destroy() {
		_ctx = null;
		_fileType = null;
	}

	
	private boolean isDynamicResource(String src) {
		int lastIndexOf = src.lastIndexOf(".");
		return _fileType.containsKey(src.substring(lastIndexOf));
	}
	public void service(HttpServletRequest request,
			HttpServletResponse response, String pi) throws ServletException,
			IOException {
		final Session sess = Sessions.getCurrent(false);
		if (sess == null) {
			response.setIntHeader("ZK-Error", HttpServletResponse.SC_GONE);
			return;
		}
		final String dtid = request.getParameter("dtid");
		String src = request.getParameter("src");
		if (src != null) {
			//Not sure whether a name might contain ;jsessionid or similar
			//But we handle this case: x.y;z
			final int j = src.lastIndexOf(';');
			if (j > 0) {
				final int k = src.lastIndexOf('.');
				if (k >= 0 && j > k && k > src.lastIndexOf('/'))
					src = src.substring(0, j);
			}
		}
		final WebApp wapp = sess.getWebApp();
		final WebAppCtrl wappc = (WebAppCtrl) wapp;
		final Desktop desktop = wappc.getDesktopCache(sess).getDesktop(dtid);
		final Execution exec = new ExecutionImpl(_ctx, request, response,
				desktop, null);

		Object updctx = null;
		try {

			if (desktop == null) {
				response.setIntHeader("ZK-Error", HttpServletResponse.SC_GONE);
				return;
			}
			if (isDynamicResource(src)) {
				updctx = ((WebAppCtrl)desktop.getWebApp()).getUiEngine().startUpdate(exec);
				
				StringBuilder sb = new StringBuilder(512);
				final RequestInfo ri = new RequestInfoImpl(wapp, sess, desktop,
						request, PageDefinitions.getLocator(wapp, src));
				PageDefinition pageDefinition = exec.getPageDefinition(src);
				final Page page = WebManager.newPage(wappc.getUiFactory(), ri,
						pageDefinition, response, src);
				StringWriter fakeWriter = new StringWriter(1024 * 8);
				wappc.getUiEngine().execNewPage(exec,
						exec.getPageDefinition(src), page, fakeWriter);
	
				fakeWriter = null;
				JSONArray result = ((WebAppCtrl)desktop.getWebApp())
						.getUiEngine().finishUpdate(updctx);
				sb.append(result.toString());
				
	
				ServletOutputStream outer = response.getOutputStream();
				outer.write(sb.toString().getBytes("UTF-8"));
				outer.flush();
			} else throw new UiException(); // we handle it at "catch" area

		} catch (ComponentNotFoundException ex) {
			// possible because view might be as late as origin comp is gone
			response.sendError(HttpServletResponse.SC_GONE,
					Messages.get(MZk.PAGE_NOT_FOUND, src));
			return;
		} catch (UiException ex) {
			String filePath = src;
			if (filePath.charAt(0) != '/' && filePath.charAt(0) != '~')
				filePath = "/" + filePath;
			
			// load from classpath
			if (filePath.startsWith("~.")) {
				StringWriter fakeWriter = new StringWriter(1024 * 8);
				exec.include(fakeWriter, filePath, null, 0);
				ServletOutputStream outer = response.getOutputStream();
				outer.write(fakeWriter.toString().getBytes("UTF-8"));
				outer.flush();
			} else {
				InputStream resourceAsStream = _ctx.getResourceAsStream(filePath);
				if (resourceAsStream != null) {
					ServletOutputStream outer = response.getOutputStream();
					IOUtils.copy(resourceAsStream, outer);
					outer.flush();
				} else {
					response.sendError(HttpServletResponse.SC_GONE,
							Messages.get(MZk.PAGE_NOT_FOUND, src));
				}
			}
		} finally {
			try {
				if (updctx != null)
				((WebAppCtrl)exec.getDesktop().getWebApp()).getUiEngine().closeUpdate(updctx);
				updctx = null;
			} catch (Exception ex) { //not possible
				throw UiException.Aide.wrap(ex);
			} finally {
				Charsets.cleanup(request, Charsets.setup(request, response, "utf-8"));
			}
		}
	}

}