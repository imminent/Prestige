package test;

import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Presentation
	public interface Inner { }
	
	public static final class Implementation implements Inner { };
}
