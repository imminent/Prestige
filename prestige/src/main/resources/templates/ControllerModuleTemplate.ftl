// Generated code from Prestige. Do not modify!
package ${package};

import javax.inject.Named;

import com.imminentmeals.prestige.ControllerContract;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import dagger.Singleton;

// Dynamic imports
<#list controllers as controller>
import ${controller.interface};
</#list>

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
	
	@Provides ${controller.interface.getSimpleName()} provides${controller.interface.getSimpleName()}(
		@Named(ControllerContract.BUS) Bus bus) {
		return new ${controller.implementation}(bus);
	}
	</#list>
}
