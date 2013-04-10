package test;

import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.PresentationImplementation;

public class Test {
	@Presentation
	public interface PresentationInterface { }
	
	@PresentationImplementation
	public static final class TestClass implements PresentationInterface { };
}
