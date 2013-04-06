package test;

import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Presentation(protocol = Protocol.class)
	public interface Inner { }
	
	public static final class Protocol { }
}
