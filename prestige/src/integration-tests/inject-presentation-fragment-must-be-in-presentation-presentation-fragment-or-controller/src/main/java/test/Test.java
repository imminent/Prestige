package test;

import android.app.Activity;

import com.imminentmeals.prestige.annotations.InjectPresentationFragment;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.PresentationFragment;

public class Test {
	public interface PresentationFragmentInterface { }
		
	@Presentation
	public interface PresentationInterface { }
	
	public static class TestClass extends Activity implements PresentationInterface {
		@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;
	}
}
