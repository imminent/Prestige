package test;

import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Controller(presentation = PresentationInterface.class)
	public interface ControllerInterface { }
	
	@Presentation
	public interface PresentationInterface { }
}
