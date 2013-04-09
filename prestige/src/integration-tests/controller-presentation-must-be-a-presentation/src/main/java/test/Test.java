package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Controller(presentation = PresentationInterface.class)
	public interface Inner extends ControllerContract { }
	
	public interface PresentationInterface { }	
}
