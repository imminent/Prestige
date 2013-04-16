<#-- 
	Fields:
  		package - String
  		target - Element
  		variableName - String
  		className - String
-->
// Generated code from Prestige. Do not modify!
package ${package};

import com.imminentmeals.prestige.Prestige.Finder;
import com.imminentmeals.prestige.ControllerContract;

/**
 * <p>Injects the Data Source into {@link ${target}}'s ${variableName}.</p>
 */
public class ${className} {

	/**
	 * <p>Injects the Data Source into {@link ${target}}'s ${variableName}.</p>
	 * @param finder The finder that specifies how to retrieve the Segue Controller from the target
	 * @param target The target of the injection
	 */
	public static void injectDataSource(Finder finder, ${target} target) {
		target.${variableName} = finder.findSegueControllerApplication(target).segueController().dataSource(target.getClass());
	}
}
