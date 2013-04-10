package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	public static final class TestController implements ControllerInterface {
		public void attachPresentation(Object presentation) { }
	}
	
	@Controller(presentation = PresentationInterface.class)
	public interface ControllerInterface extends ControllerContract { }
	
	@Presentation
	public interface PresentationInterface { }
}
