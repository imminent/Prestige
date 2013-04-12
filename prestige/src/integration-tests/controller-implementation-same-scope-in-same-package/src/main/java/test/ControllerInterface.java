package test;

import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;

@Controller(presentation = PresentationInterface.class)
public interface ControllerInterface extends ControllerContract { }
