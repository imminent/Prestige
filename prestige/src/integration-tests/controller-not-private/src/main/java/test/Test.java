package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Controller(PresentationInterface.class)
	private interface Inner extends ControllerContract { }
	
	@Presentation
	public interface PresentationInterface { }
}
