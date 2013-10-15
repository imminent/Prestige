package com.imminentmeals.prestige;

import android.app.Activity;

import java.io.IOException;

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
	
	void attachPresentationFragment(Activity activity, Object presentation_fragment, String tag);
	
	void registerForControllerBus(Activity activity);
	
	void unregisterForControllerBus(Activity activity);

    <T> void store(T object) throws IOException;
}
