package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.PresentationImplementation;

import android.app.Activity;

public class Test {
	@Controller(presentation = PresentationInterface.class)
	public static class TestController implements ControllerContract { 
		public void attachPresentation(Object presentation) { }
	}
	
	@Presentation
	public interface PresentationInterface { }
}
