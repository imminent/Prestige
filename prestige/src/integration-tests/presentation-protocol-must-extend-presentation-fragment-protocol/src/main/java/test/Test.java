package test;

import android.app.Activity;

import com.imminentmeals.prestige.annotations.InjectPresentationFragment;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.PresentationFragment;
import com.imminentmeals.prestige.annotations.PresentationImplementation;

public class Test {
	@PresentationFragment(protocol = PresentationFragmentProtocol.class)
	public interface PresentationFragmentInterface { }
	
	public interface PresentationFragmentProtocol { }
	
	@Presentation(protocol = PresentationProtocol.class)
	public interface PresentationInterface { }
	
	public interface PresentationProtocol { }
	
	@PresentationImplementation
	public static class TestPresentation extends Activity implements PresentationInterface {
		@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;
	}
}
