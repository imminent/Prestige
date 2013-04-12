package test.different;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;

@Controller(presentation = DifferentPackagePresentationInterface.class)
public interface DifferentPackageControllerInterface extends ControllerContract { }