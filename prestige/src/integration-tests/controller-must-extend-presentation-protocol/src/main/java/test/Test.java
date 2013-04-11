package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Controller
	public interface TestController extends ControllerContract {
		public void attachPresentation(Object presentation);
	}
	
	@Presentation(protocol = Protocol.class)
	public interface TestPresentation { }
	
	public interface Protocol { }
}
