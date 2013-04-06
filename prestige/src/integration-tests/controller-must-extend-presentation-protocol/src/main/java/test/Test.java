package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Controller(PresentationInterface.class)
	public interface Inner extends ControllerContract { }
	
	@Presentation(protocol = Protocol.class)
	public interface PresentationInterface { }
	
	public interface Protocol { }	
}
