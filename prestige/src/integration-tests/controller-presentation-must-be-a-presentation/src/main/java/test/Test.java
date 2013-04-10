package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Controller(presentation = NoPresentationAnnotationInterface.class)
	public interface ControllerInterface extends ControllerContract { }
	
	public interface NoPresentationAnnotationInterface { }	
}
