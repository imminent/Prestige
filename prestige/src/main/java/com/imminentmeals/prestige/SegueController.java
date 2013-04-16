package com.imminentmeals.prestige;

import android.app.Activity;

/**
 *
 * @author Dandre Allison
 */
public interface SegueController {

	<T> T dataSource(Class<?> target);

	void sendMessage(Object message);
	
	void createController(Activity activity); 
	
	void didDestroyActivity(Activity activity);
}
