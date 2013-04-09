package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Controller(presentation = PresentationInterface.class)
	public static class Inner implements ControllerContract { 
		public void attachPresentation(Object presentation) { }
	}
	
	@Presentation
	public interface PresentationInterface { }
}
