package test;

import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.Presentation;
import com.squareup.otto.Bus;

@ControllerImplementation(TEST)
public final class TestController implements ControllerInterface {
	
	public TestController(Bus bus) { }
	
	public void attachPresentation(Object presentation) { }
}

