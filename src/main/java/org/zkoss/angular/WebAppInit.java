/** WebAppInit.java.

	Purpose:
		
	Description:
		
	History:
		2:58:23 PM Jul 21, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.angular;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.Converter;
import org.zkoss.bind.impl.SystemConverters;
import org.zkoss.lang.Classes;
import org.zkoss.lang.Library;
import org.zkoss.zk.ui.WebApp;


/**
 * Angular module's WebAppInit implementation
 * @author jumperchen
 *
 */
public class WebAppInit implements org.zkoss.zk.ui.util.WebAppInit {
	private static final Logger _log = LoggerFactory.getLogger(WebAppInit.class);
	public void init(WebApp wapp) throws Exception {
		String property = Library.getProperty("org.zkoss.angular.jsonConverter.class", "org.zkoss.angular.JacksonConverter");
		try {
			SystemConverters.set(AngularBinder.JSON_CONVERTER, (Converter)Classes.newInstanceByThread(property));
			AuInclude.init(wapp);
		} catch (Exception x) {
			_log.error(x.getMessage(),x);
		}
	}

}
