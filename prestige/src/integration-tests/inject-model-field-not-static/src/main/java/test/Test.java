package test;

import com.imminentmeals.prestige.annotations.Model;
import com.imminentmeals.prestige.annotations.ModelImplementation;
import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.InjectModel;

public class Test {
	@Model
	public interface ModelInterface { }
	
	@ModelImplementation(TEST)
	public class TestModel implements ModelInterface { }
	
	@Controller
	public interface ControllerInterface extends ControllerContract { }
	
	@ControllerImplementation(TEST)
	public class TestController {
		@InjectModel /* package */static ModelInterface test;
		
		@Override
		public void attachPresentation(Object presentation) { }
	}
}
