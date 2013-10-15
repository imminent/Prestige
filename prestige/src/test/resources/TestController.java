package test;

import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.ControllerImplementation;

@ControllerImplementation(TEST)
public final class TestController implements ControllerInterface { }

