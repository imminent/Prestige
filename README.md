# The Pledge

<state of Android development>

# The Turn

While "magic is for children", sometimes careful planning can turn the oridinary into something magical.

<what that code becomes>

# The Prestige

With `Prestige.conjureSegueController(String)` and an `Application` that implements `SegueControllerApplication`, you are able to create the Controllers and inject Data Sources using `Prestige.conjureController(Activity)` and `@InjectDataSource`. You should make sure to `Prestige.vansihController(Activity)` when the `Activity` is destroyed. This setup can be done in an `Application.ActivityLifecycleCallbacks`

```
public class PrestigeCallbacks implements Application.ActivityLifecycleCallbacks {
	
	@Override
	public void onActivityCreated(Activity activity, Bundle _) {
		Prestige.conjureController(activity);
	}
	
	@Override
	public void onActivityDestroyed(Activity activity) {
		Prestige.vanishController(activity);
	}
	
	// … Remaining methods are empty
}
```

Note: Prestige currently requires Ice Cream Sandwich to work, but backward-compatibility can be included.