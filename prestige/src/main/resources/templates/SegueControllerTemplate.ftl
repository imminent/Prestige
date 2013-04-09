// Generated code from Prestige. Do not modify!
package com.imminentmeals.prestige;

import static com.google.common.collect.Maps.newHashMap;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Fragment;
import android.os.Bundle;

import com.google.common.collect.ImmutableMap;
import com.imminentmeals.prestige.ControllerContract;
import com.squareup.otto.Bus;

import dagger.Module;
import dagger.ObjectGraph;

<#if imports?has_content>
// Dynamic imports
<#list imports as class>
import ${class};
</#list>
</#if>

/**
 * <p>A Segue Controller that handles getting the appropriate Controller for the current Presentation, and establishing the
 * line of communication between them. Register the {@link SegueController} with 
 * {@link android.app.Application#registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks)} to enable its functionality.</p>
 */
public class SegueController implements ActivityLifecycleCallbacks {
	/** Bus over which Presentations communicate to their Controllers */
	@Inject @Named(ControllerContract.BUS)/* package */Bus controller_bus;
	<#if controllers?has_content>
	// Controller Injections 
	<#list controllers as controller>
	/** Provider for instances of the {@link ${controller.className}} Controller */
	@Inject /* package */Provider<${controller.className}> ${controller.variableName};
	</#list>
	</#if>
	
/* Constructor */
	/**
	 * <p>Constructs a {@link SegueController}. It still has to be registered with 
	 * {@link android.app.Application#registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks)} before it performs its 
	 * function.</p>
	 */
	public SegueController() {
		final ObjectGraph object_graph = ObjectGraph.create(new ModelViewControllerModule());
		object_graph.inject(this);
		_presentation_controllers = new ImmutableMap.Builder<Class<? extends Activity>, Provider<? extends ControllerContract>>()
			<#if controllers?has_content>
			// Presentation -> injected Controller 
			<#list controllers as controller>
			.put(${controller.presentationImplementation}, ${controller.variableName})
			</#list>
			</#if>
			.build();
		_controllers = newHashMap();
	}
	
/* Activity Lifecycle Callbacks */
	@Override
	public void onActivityCreated(Activity activity, Bundle _) {
		if (!_presentation_controllers.containsKey(activity.getClass())) return;
		
		final ControllerContract controller = _presentation_controllers.get(activity.getClass()).get();
		controller.attachPresentation(activity);
		_controllers.put(activity.getClass(), controller);
	}
	
	@Override public void onActivityStarted(Activity _) { }
	@Override public void onActivityResumed(Activity _) { }
	@Override public void onActivityPaused(Activity _) { }
	@Override public void onActivityStopped(Activity _) { }
	
	@Override 
	public void onActivityDestroyed(Activity activity) {
		_controllers.remove(activity.getClass());
	}
	
	@Override public void onActivitySaveInstanceState(Activity _, Bundle __) { }
	
/* Exposed Controller API */
	/**
	 * <p>Retrieves the {@linkplain Activity Presentation}'s Controller.</p>
	 * @param presentation The given Presentation
	 * @return The given Presentation's Controller
	 */ 
	@SuppressWarnings("unchecked")
	public static <T> T controller(Activity presentation) {
		return (T) segueController(presentation)._controllers.get(presentation.getClass());
	}
	
	/**
	 * <p>Retrieves the {@linkplain Fragment Presentation Fragment}'s Controller.</p>
	 * @param presentation_fragment The given Presentation Fragment
	 * @return The given Presentation Fragment's Controller
	 */
	public static <T> T controller(Fragment presentation_fragment) {
		final Activity presentation = presentation(presentation_fragment);
		if (!segueController(presentation)._controllers.containsKey(presentation.getClass()))
			throw new IllegalArgumentException("Fragment " + presentation_fragment.getClass().getName()
					+ " isn't attached to a Presentation");
		return controller(presentation);
	}
	
	/**
	 * <p>Sends the given message from the {@linkplain Activity Presentation} to its Controller.</p>
	 * @param presentation The Presentation from which the message was sent
	 * @param message The message to send
	 */
	public static void sendMessage(Activity presentation, Object message) {
		segueController(presentation).controller_bus.post(message);
	}
	
	/**
	 * <p>Sends the given message from the {@linkplain Fragment presentation_fragment} to its Controller.</p>
	 * @param presentation_fragment The Presentation Fragment from which the message was sent
	 * @param message The message to send
	 */
	public static void sendMessage(Fragment presentation_fragment, Object message) {
		sendMessage(presentation(presentation_fragment), message);
	}
	
/* Private Helpers */
	/**
	 * <p>Retrieves the {@link SegueController}.</p>
	 * @param presentation {@linkplain Activity Presentation} from which to retrieve the Segue Controller 
	 * @return The Segue Controller
	 */
	@SuppressWarnings("unchecked")
	private static SegueController segueController(Activity presentation) {
		return (SegueController) ((SegueControllerApplication) presentation.getApplication()).segueController();
	}
	
	@SuppressWarnings("unchecked")
	private static Activity presentation(Fragment presentation_fragment) {
		final Activity presentation = presentation_fragment.getActivity();
		if (presentation == null)
			throw new IllegalStateException("Fragment " + presentation_fragment.getClass().getName()
				+ " isn't attached to an Activity");
		
		return presentation;
	}
	
	/** Provides the Controller implementation for the given Activity */
	private final ImmutableMap<Class<? extends Activity>, Provider<? extends ControllerContract>> _presentation_controllers;
	/** Maintains the set of currently used controllers */
	private final Map<Class<? extends Activity>, ControllerContract> _controllers;
}
