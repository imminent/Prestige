package com.imminentmeals.prestige;

import android.app.Activity;

import com.google.gson.InstanceCreator;

import java.io.IOException;

import javax.annotation.Nonnull;

import timber.log.Timber;

/**
 *
 * @author Dandre Allison
 */
public interface SegueController {

	<T> T dataSource(Class<?> target);

	void sendMessage(Object message);
	
	void createController(Activity activity);

    <M, I extends M> I createModel(Class<M> model_interface);
	
	void didDestroyActivity(Activity activity);
	
	void attachPresentationFragment(Activity activity, Object presentation_fragment, String tag);
	
	void registerForControllerBus(Activity activity);
	
	void unregisterForControllerBus(Activity activity);

    <T> void store(T object) throws IOException;

    @Nonnull Timber timber();

    void storeController(Activity activity);

    @Nonnull <M, I extends M> InstanceCreator<I> instanceCreator(Class<M> type);
}
