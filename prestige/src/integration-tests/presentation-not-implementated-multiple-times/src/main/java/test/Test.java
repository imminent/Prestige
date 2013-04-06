package test;

import android.app.Activity;

import com.imminentmeals.prestige.annotations.Presentation;

public class Test {
	@Presentation
	public interface Inner { }
	
	public static final class Implementation0 extends Activity implements Inner;
	
	public static final class Implementation1 extends Activity implements Inner;
}
