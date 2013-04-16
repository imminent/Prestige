package test;

import android.app.Activity;

import com.imminentmeals.prestige.annotations.InjectDataSource;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.PresentationImplementation;

public class Test {
	public interface Protocol { }
	
	@Presentation(protocol = Protocol.class)
	public interface PresentationInterface { }
	
	@PresentationImplementation
	public static final class TestClass extends Activity implements PresentationInterface { 
		@InjectDataSource static Protocol data_source;
	}
}
