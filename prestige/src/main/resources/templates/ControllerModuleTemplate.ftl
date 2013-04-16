<#-- 
	Fields:
		package - String
  		controllers - List:
  			interface - Element
  			implementation - Element
  			presentationImplementation - Element
  		className - String
-->
// Generated code from Prestige. Do not modify!
package ${package};

import javax.inject.Named;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige._SegueController;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * <p>Module for injecting:
 * <ul>
<#list controllers as controller>
 * <li>{@link ${controller.interface}}</li>
</#list>
 * </ul>
 */
 @Module(
	entryPoints = {
		_SegueController.class,
		<#list controllers as controller>
	    ${controller.implementation}.class<#if controller_has_next>,</#if>
	    </#list>
    }
)
public class ${className} {

	@Provides @Singleton @Named(ControllerContract.BUS) Bus providesControllerBus() {
		return new Bus("Controller Bus");
	}
	<#list controllers as controller>
	
	@Provides ${controller.interface} provides${controller.interface.getSimpleName()}() {
		return new ${controller.implementation}();
	}
	</#list>
}
