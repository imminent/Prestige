package test.different;

import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;

import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.squareup.otto.Bus;

@ControllerImplementation(TEST)
public final class DifferentPackageController implements DifferentPackageControllerInterface {
	
	public DifferentPackageController(Bus bus) { }
	
	public void attachPresentation(Object presentation) { }
}
