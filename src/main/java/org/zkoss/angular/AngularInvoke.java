/** AngularInvoke.java.

	Purpose:
		
	Description:
		
	History:
		6:40:33 PM Jul 31, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.angular;

import org.zkoss.zk.au.AuResponse;
import org.zkoss.zk.ui.Component;

/**
 * Like AuInvoke, but with different command name
 * @author jumperchen
 *
 */
public class AngularInvoke extends AuResponse {

	public AngularInvoke(Component comp, String function, Object arg1, Object arg2) {
		super("ngInvoke", comp, new Object[] {comp, function,
			arg1, arg2});
	}

}
