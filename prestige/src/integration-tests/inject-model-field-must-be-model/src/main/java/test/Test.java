package test;

import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;
import com.imminentmeals.prestige.annotations.InjectModel;
import com.imminentmeals.prestige.ControllerContract;

public class Test {
	@Controller
	public interface ControllerInterface extends ControllerContract { }
	
	@ControllerImplementation(TEST)
	public class TestNotController {
		@InjectModel /* package */Object test;
		
		@Override
		public void attachPresentation(Object presentation) { }
	}
}
