package com.imminentmeals.prestige;

import static com.google.common.collect.Maps.newLinkedHashMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.Nonnull;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.imminentmeals.prestige.codegen.AnnotationProcessor;


/**
 * 
 * @author Dandre Allison
 */
public final class Prestige {
	
	/**
	 * <p>Defines the API for finding the {@link SegueControllerApplication} given an {@link Activity} or {@link Fragment}.</p>
	 * @author Dandre Allison
	 */
	public enum Finder {
		/**
		 * <p>Finds the {@link SegueControllerApplication} given a {@link Fragment}.</p>
		 */
		FRAGMENT {
			@Override
			public SegueControllerApplication findSegueControllerApplication(Object source) {
				return ACTIVITY.findSegueControllerApplication(((Fragment) source).getActivity());
			}
		},
		/**
		 * <p>Finds the {@link SegueControllerApplication} given a {@link Activity}.</p>
		 */
		ACTIVITY {
			@Override
			public SegueControllerApplication findSegueControllerApplication(Object source) {
				return (SegueControllerApplication) ((Activity) source).getApplication();
			}
		};

		/** 
		 * <p>Finds the {@link SegueControllerApplication} given a valid source.</p> 
		 * @param source The source of the SegueControllerApplication
		 * @return The SegueControllerApplication
		 */
		public abstract SegueControllerApplication findSegueControllerApplication(Object source);
	}
	
	/**
	 * <p>Injects a Data Source into the given {@link Activity}.</p>
	 * @param activity The target of the injection
	 */
	public static void conjureController(@Nonnull Activity activity) {
		// Creates Controller
		Finder.ACTIVITY.findSegueControllerApplication(activity).segueController().createController(activity);
		injectDataSource(Finder.ACTIVITY, activity);
	}
	
	/**
	 * <p>Injects a Data Source into the given {@link Fragment}.</p>
	 * @param fragment The target of the injection
	 */
	public static void injectDataSource(@Nonnull Fragment fragment) {
		injectDataSource(Finder.FRAGMENT, fragment);
	}
	
	/**
	 * <p>Sends the given message on the Controller Bus.</p>
	 * @param activity The Activity that sent the message
	 * @param message The message to send
	 */
	public static void sendMessage(@Nonnull Activity activity, @Nonnull Object message) {
		Finder.ACTIVITY.findSegueControllerApplication(activity).segueController().sendMessage(message);
	}
	
	/**
	 * <p>Sends the given message on the Controller Bus.</p>
	 * @param fragment The Fragment that sent the message
	 * @param message The message to send
	 */
	public static void sendMessage(@Nonnull Fragment fragment, @Nonnull Object message) {
		Finder.FRAGMENT.findSegueControllerApplication(fragment).segueController().sendMessage(message);
	}
	  
	/**
	 * <p>Creates the {@link SegueController} set for the given implementation scope. The implementation scope specifies
	 * which version of implementations to use.</p>
	 * @param scope The implementation scope
	 * @return The Segue Controller
	 */
	public static SegueController conjureSegueController(@Nonnull String scope) {	
		try {
			final Class<?> segue_controller = Class.forName("com.imminentmeals.prestige._SegueController");
			return (SegueController) segue_controller.getConstructor(String.class).newInstance(scope);
		} catch (IllegalArgumentException exception) {
			Log.e("Prestige", "Problem with Prestige setup", exception);
		} catch (SecurityException exception) {
			Log.e("Prestige", "Problem with Prestige setup", exception);
		} catch (InstantiationException exception) {
			Log.e("Prestige", "Problem with Prestige setup", exception);
		} catch (IllegalAccessException exception) {
			Log.e("Prestige", "Problem with Prestige setup", exception);
		} catch (InvocationTargetException exception) {
			Log.e("Prestige", "Problem with Prestige setup", exception);
		} catch (NoSuchMethodException exception) {
			Log.e("Prestige", "Problem with Prestige setup", exception);
		} catch (ClassNotFoundException exception) {
			Log.e("Prestige", "Generated _SegueController cannot be found", exception);
		}
		return null;
	}
	
	/**
	 * <p>Destroys the constructed Controller for the given {@link Activity}.</p>
	 * @param activity The given Activity
	 */
	public static void vanishController(@Nonnull Activity activity) {
		Finder.ACTIVITY.findSegueControllerApplication(activity).segueController().didDestroyActivity(activity);
	}
	
	/**
	 * <p>Retrieves the {@link ActivityLifecycleCallbacks} that binds Prestige to the Activity lifecycles so that it can
	 * conjure Controllers behind the curtains.</p>
	 * @return The Prestige callbacks
	 */
	public static ActivityLifecycleCallbacks activityLifecycleCallbacks() {
		return new ActivityLifecycleCallbacks() {
			
			@Override
			public void onActivityDestroyed(Activity activity) {
				vanishController(activity);	
			}
			
			@Override
			public void onActivityCreated(Activity activity, Bundle __) {
				conjureController(activity);
			}
			
			@Override
			public void onActivityStopped(Activity _) { }
			
			@Override
			public void onActivityStarted(Activity _) { }
			
			@Override
			public void onActivitySaveInstanceState(Activity _, Bundle __) { }
			
			@Override
			public void onActivityResumed(Activity _) { }
			
			@Override
			public void onActivityPaused(Activity _) { }
		};
	}
	
	/**
	 * <p>Materializes Prestige for the given scope and binds it to the Activity lifecycles in the given Application. This is
	 * equivalent to how you would use {@link #conjureSegueController(String)} and {@link Prestige#activityLifecycleCallbacks()}
	 * in most cases.</p>
	 * @param application
	 * @param scope
	 * @return
	 */
	public static SegueController materialize(@Nonnull Application application, @Nonnull String scope) {
		application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks());
		return conjureSegueController(scope);
	}
	
	/**
	 * <p>Indicates an unexpected error occurred while trying attempting to inject a Data Source.</p>
	 * @author Dandre Allison
	 */
	@SuppressWarnings("serial")
	public static class UnableToInjectException extends RuntimeException {
		
		/**
		 * <p>Creates an {@link UnableToInjectException}.</p>
		 * @param message The detailed message
		 * @param cause The cause of the error
		 */
	    /* package */UnableToInjectException(String message, Throwable cause) {
	      super(message, cause);
	    }
	  }

/* Helpers */
	/**
	 * <p>Injects a Data Source into the given target.</p>
	 * @param finder The finder that specifies how to retrieve the Segue Controller from the target
	 * @param target The target of the injection
	 */
	/* package */static void injectDataSource(@Nonnull Finder finder, @Nonnull Object target) {
		final Class<?> target_class = target.getClass();
		try {
			final Method inject;
			if (!_INJECTORS.containsKey(target_class)) {
				final Class<?> injector = Class.forName(target_class.getName() + AnnotationProcessor.DATA_SOURCE_SUFFIX);
				inject = injector.getMethod("injectDataSource", Finder.class, target_class);
				_INJECTORS.put(target_class, inject);
			} else
				inject = _INJECTORS.get(target_class);
			// Allows for no-ops when there's nothing to inject.
			if (inject != null)
				inject.invoke(null, finder, target);
		} catch (ClassNotFoundException _) {
			// Allows injectDataSource to be called on targets without a data source
			_INJECTORS.put(target_class, _NO_OP);
		} catch (RuntimeException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new UnableToInjectException("Unable to inject Data Source for " + target, exception instanceof InvocationTargetException? ((InvocationTargetException) exception).getTargetException() : exception);
		}
	}
	
/* Private Constructor */
	/** <p>Prevents the {@link Prestige} from being constructed.</p> */
	private Prestige() { }
	
	/** Caches the Data Source injects previously found to reduce reflection impact */
	private static final Map<Class<?>, Method> _INJECTORS = newLinkedHashMap();
	/** No Data Source to inject */
	private static final Method _NO_OP = null;
}
