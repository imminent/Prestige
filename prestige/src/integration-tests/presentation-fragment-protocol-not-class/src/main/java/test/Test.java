package test;

import com.imminentmeals.prestige.annotations.PresentationFragment;

public class Test {
	@PresentationFragment(protocol = Protocol.class)
	public interface PresentationFragmentInterface { }
	
	public static final class Protocol { }
}
