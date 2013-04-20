<#-- 
	Fields:
  		controllers - List:
  			element - Element
  			variableName - String
  			presentationImplementation - Element
  		modules - List:
  			name - String
  			scope - String
-->
// Generated code from Prestige. Do not modify!
package com.imminentmeals.prestige;

import static com.google.common.collect.Maps.newHashMap;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import android.app.Activity;

import com.google.common.collect.ImmutableMap;
import com.imminentmeals.prestige.ControllerContract;
import com.squareup.otto.Bus;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * <p>A Segue Controller that handles getting the appropriate Controller for the current Presentation, and communicating with the
 * Controller Bus.</p>
 */
public class _SegueController implements SegueController {
	/** Bus over which Presentations communicate to their Controllers */
	@Inject @Named(ControllerContract.BUS)/* package */Bus controller_bus;
	<#if controllers?has_content>
	// Controller Injections 
	<#list controllers as controller>
	/** Provider for instances of the {@link ${controller.element}} Controller */
	@Inject /* package */Provider<${controller.element}> ${controller.variableName};
	</#list>
	</#if>
	
/* Constructor */
	/**
	 * <p>Constructs a {@link SegueController}.</p>
	 */
	public _SegueController(String scope) {
		Object module = null;
		<#list modules as module>
		<#if module_index gt 0>else</#if> if (scope.equals("${module.scope}"))
			module = new ${module.name}();
		</#list>
		final ObjectGraph object_graph = ObjectGraph.create(module);
		object_graph.inject(this);
		_presentation_controllers = ImmutableMap.<Class<?>, Provider<? extends ControllerContract>>builder()
			<#if controllers?has_content>
			// Presentation -> injected Controller 
			<#list controllers as controller>
			.put(${controller.presentationImplementation}.class, ${controller.variableName})
			</#list>
			</#if>
			.build();
		_controllers = newHashMap();
	}
	
/* SegueController Contract */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T dataSource(Class<?> target) {
		return (T) _controllers.get(target);
	}
	
	@Override
	public void sendMessage(Object message) {
		controller_bus.post(message);
	}
	
	@Override
	public void createController(Activity activity) {
		final Class<?> activity_class = activity.getClass();
		if (!_presentation_controllers.containsKey(activity_class)) return;
		
		final ControllerContract controller = _presentation_controllers.get(activity_class).get();
		controller.attachPresentation(activity);
		controller_bus.register(controller);
		_controllers.put(activity_class, controller);	
	}
	
	@Override
	public void didDestroyActivity(Activity activity) {
		final Class<?> activity_class = activity.getClass();
		if (!_presentation_controllers.containsKey(activity_class)) return;
		
		controller_bus.unregister(_controllers.get(activity_class));
		_controllers.remove(activity_class);
	}
	
	/** Provides the Controller implementation for the given Presentation Implementation */
	private final ImmutableMap<Class<?>, Provider<? extends ControllerContract>> _presentation_controllers;
	/** Maintains the Controller references as they are being used */
	private final Map<Class<?>, ControllerContract> _controllers;
}
