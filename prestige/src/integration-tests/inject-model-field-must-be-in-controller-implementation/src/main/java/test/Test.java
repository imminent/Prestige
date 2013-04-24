package test;

import com.imminentmeals.prestige.annotations.Model;
import com.imminentmeals.prestige.annotations.ModelImplementation;
import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;
import com.imminentmeals.prestige.annotations.InjectModel;


public class Test {
	@Model
	public interface ModelInterface { }
	
	@ModelImplementation(TEST)
	public class TestModel implements ModelInterface { }
	
	public class TestNotController {
		@InjectModel /* package */ModelInterface test;
	}
}
