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
	
	<T> T createModel(Class<T> model_interface);
	
	void didDestroyActivity(Activity activity);
}
