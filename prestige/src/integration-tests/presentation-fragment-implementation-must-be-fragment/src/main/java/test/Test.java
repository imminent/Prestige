package test;

import com.imminentmeals.prestige.annotations.PresentationFragment;
import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;

public class Test {
	@PresentationFragment
	public interface PresentationFragmentInterface { }
	
	@PresentationFragmentImplementation
	public static final class TestPresentationFragment implements PresentationFragmentInterface { };
}
