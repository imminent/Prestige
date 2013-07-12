package test;

import android.app.Activity;

import com.imminentmeals.prestige.annotations.InjectPresentationFragment;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.PresentationFragment;
import com.imminentmeals.prestige.annotations.PresentationImplementation;

public class Test {
	public interface PresentationFragmentInterface { }
		
	@Presentation
	public interface PresentationInterface { }
	
	@PresentationImplementation
	public static class TestPresentation extends Activity implements PresentationInterface {
		@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;
	}
}
