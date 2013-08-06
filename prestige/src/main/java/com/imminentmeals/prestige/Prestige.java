package com.imminentmeals.prestige;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.imminentmeals.prestige.codegen.AnnotationProcessor;
import com.imminentmeals.prestige.codegen.Finder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.google.common.collect.Maps.newHashMap;

/**
 * 
 * @author Dandre Allison
 */
public final class Prestige {
	
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
	 * <p>Injects the Presentation Fragments for the given display into the given Presentation.</p>
	 * @param activity The given Presentation
	 * @param display The given display
	 */
	public static void injectPresentationFragments(@Nonnull Activity activity, int display) {
		injectPresentationFragments(Finder.ACTIVITY, display, activity);
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
	 * <p>Sends the given message on the Controller Bus.</p>
	 * @param message The message to send
	 */
	public static void sendMessage(@Nonnull Object message) {
		if (_segue_controller == null)
			throw new IllegalStateException("Attempting to send message before Segue Controller was created " +
					"(Prestige.conjureSegueController(String)).");
		_segue_controller.sendMessage(message);
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
			_segue_controller = (SegueController) segue_controller.getConstructor(String.class).newInstance(scope);
			return _segue_controller;
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
	
	public static void registerForControllerBus(@Nonnull Activity activity) {
		Finder.ACTIVITY.findSegueControllerApplication(activity).segueController().registerForControllerBus(activity);
	}
	
	public static void unregisterForControllerBus(@Nonnull Activity activity) {
		Finder.ACTIVITY.findSegueControllerApplication(activity).segueController().unregisterForControllerBus(activity);
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
			public void onActivityResumed(Activity activity) {
				registerForControllerBus(activity);
			}
			
			@Override
			public void onActivityPaused(Activity activity) {
				unregisterForControllerBus(activity);
			}
			
			@Override
			public void onActivityStopped(Activity _) { }
			
			@Override
			public void onActivityStarted(Activity _) { }
			
			@Override
			public void onActivitySaveInstanceState(Activity _, Bundle __) { }
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
	 * <p>Retrieves the Model Implementation for the given Model.</p>
	 * @param model_interface The given Model
	 * @return The implementation of the given Model
	 */
	public static <T> T getModel(Class<T> model_interface) {
		if (_segue_controller == null)
			throw new IllegalStateException("Attempting to get Model before Segue Controller was created " +
					"(Prestige.conjureSegueController(String)).");
		return _segue_controller.createModel(model_interface);
	}
	
	/**
	 * <p>Attaches the given Presentation Fragment to the Controller for the give Presentation.</p>
	 * <p>It is recommended to do this in {@link Activity#onAttachFragment(Fragment)}.</p>
	 * @param activity
	 * @param fragment
	 */
	public static void attachPresentationFragment(@Nonnull Activity activity, @Nonnull Fragment fragment) {
		if (_segue_controller == null)
			throw new IllegalStateException("Attempting to attach Presentation Fragment before Segue Controller was created " +
					"(Prestige.conjureSegueController(String)).");
		if (fragment.getTag() == null)
			throw new IllegalArgumentException("Unable to inject Presentation Fragments without a tag: " + 
					fragment.getClass().getCanonicalName());
		_segue_controller.attachPresentationFragment(activity, fragment, fragment.getTag());
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
	/* package */static void injectModels(@Nonnull SegueController segue_controller, @Nonnull Object target) {
		final Class<?> target_class = target.getClass();
		try {
			final Method inject;
			if (!_MODEL_INJECTORS.containsKey(target_class)) {
				final Class<?> injector = Class.forName(target_class.getName() + AnnotationProcessor.MODEL_INJECTOR_SUFFIX);
				inject = injector.getMethod("injectModels", SegueController.class, target_class);
				_MODEL_INJECTORS.put(target_class, inject);
			} else
				inject = _MODEL_INJECTORS.get(target_class);
			// Allows for no-ops when there's nothing to inject
			if (inject != null)
				inject.invoke(null, segue_controller, target);
		} catch (ClassNotFoundException _) {
			// Allows injectModels to be called on targets without injected Models
			_MODEL_INJECTORS.put(target_class, _NO_OP);
		} catch (RuntimeException exception) {
			throw exception;
		} catch (InvocationTargetException exception) {
			throw new UnableToInjectException("Unable to inject Models for " + target, exception.getTargetException());
		} catch (Exception exception) {
			throw new UnableToInjectException("Unable to inject Models for " + target, exception);
		}
	}
	
	/**
	 * <p>Injects a Data Source into the given target.</p>
	 * @param finder The finder that specifies how to retrieve the Segue Controller from the target
	 * @param target The target of the injection
	 */
	/* package */static void injectDataSource(@Nonnull Finder finder, @Nonnull Object target) {
		final Class<?> target_class = target.getClass();
		try {
			final Method inject;
			if (!_DATA_SOURCE_INJECTORS.containsKey(target_class)) {
				final Class<?> injector = Class.forName(target_class.getName() + AnnotationProcessor.DATA_SOURCE_INJECTOR_SUFFIX);
				inject = injector.getMethod("injectDataSource", Finder.class, target_class);
				_DATA_SOURCE_INJECTORS.put(target_class, inject);
			} else
				inject = _DATA_SOURCE_INJECTORS.get(target_class);
			// Allows for no-ops when there's nothing to inject
			if (inject != null)
				inject.invoke(null, finder, target);
		} catch (ClassNotFoundException _) {
			// Allows injectDataSource to be called on targets without a data source
			_DATA_SOURCE_INJECTORS.put(target_class, _NO_OP);
		} catch (RuntimeException exception) {
			throw exception;
		} catch (InvocationTargetException exception) {
			throw new UnableToInjectException("Unable to inject Data Source for " + target, exception.getTargetException());
		} catch (Exception exception) {
			throw new UnableToInjectException("Unable to inject Data Source for " + target, exception);
		}
	}

    /**
     * <p>Injects a Presentation into the given target.</p>
     * @param target The target of the injection
     */
    /* package */static void injectPresentation(@Nonnull Object target, @Nonnull Object presentation) {
        final Class<?> target_class = target.getClass();
        try {
            final Method inject;
            if (!_PRESENTATION_INJECTORS.containsKey(target_class)) {
                final Class<?> injector = Class.forName(target_class.getName() + AnnotationProcessor.PRESENTATION_INJECTOR_SUFFIX);
                inject = injector.getMethod("injectPresentation", target_class, Object.class);
                _PRESENTATION_INJECTORS.put(target_class, inject);
            } else
                inject = _PRESENTATION_INJECTORS.get(target_class);
            // Allows for no-ops when there's nothing to inject
            if (inject != null)
                inject.invoke(null, target, presentation);
        } catch (ClassNotFoundException _) {
            // Allows injectPresentation to be called on targets without a presentation field
            _PRESENTATION_INJECTORS.put(target_class, _NO_OP);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (InvocationTargetException exception) {
            throw new UnableToInjectException("Unable to inject Presentation for " + target, exception.getTargetException());
        } catch (Exception exception) {
            throw new UnableToInjectException("Unable to inject Presentation for " + target, exception);
        }
    }
	
	/**
	 * <p>Injects a Presentation Fragment into the given target.</p>
	 * @param target The target of the injection
	 */
	/* package */static void injectPresentationFragments(@Nonnull Finder finder, int display, @Nonnull Object target) {
		final Class<?> target_class = target.getClass();
		try {
			final Method inject;
			if (!_PRESENTATION_FRAGMENT_INJECTORS.containsKey(target_class)) {
				final Class<?> injector = Class.forName(target_class.getName() + AnnotationProcessor.PRESENTATION_FRAGMENT_INJECTOR_SUFFIX);
				inject = injector.getMethod("injectPresentationFragments", Finder.class, int.class, target_class);
				_PRESENTATION_FRAGMENT_INJECTORS.put(target_class, inject);
			} else
				inject = _PRESENTATION_FRAGMENT_INJECTORS.get(target_class);
			// Allows for no-ops when there's nothing to inject
			if (inject != null) {
				inject.invoke(null, finder, display, target);
			}
		} catch (ClassNotFoundException _) {
			// Allows injectDataSource to be called on targets without a data source
			_PRESENTATION_FRAGMENT_INJECTORS.put(target_class, _NO_OP);
		} catch (RuntimeException exception) {
			throw exception;
		} catch (InvocationTargetException exception) {
			throw new UnableToInjectException("Unable to inject Presentation Fragment for " + target, 
					                          exception.getTargetException());
		} catch (Exception exception) {
			throw new UnableToInjectException("Unable to inject Presentation Fragment for " + target, exception);
		}
	}
	
	/* package */static void attachPresentationFragment(@Nonnull Object target, @Nonnull Object presentation_fragment, 
			@Nonnull String tag) {
		final Class<?> target_class = target.getClass();
		try {
			final Method attach;
			if (!_ATTACHERS.containsKey(target_class)) {
				final Class<?> injector = Class.forName(target_class.getName() + AnnotationProcessor.PRESENTATION_FRAGMENT_INJECTOR_SUFFIX);
				attach = injector.getMethod("attachPresentationFragment", target_class, Object.class, String.class);
				_ATTACHERS.put(target_class, attach);
			} else
				attach = _ATTACHERS.get(target_class);
			// Allows for no-ops when there's nothing to inject
			if (attach != null) {
				attach.invoke(null, target, presentation_fragment, tag);
			}
		} catch (ClassNotFoundException _) {
			// Allows injectDataSource to be called on targets without a data source
			_ATTACHERS.put(target_class, _NO_OP);
		} catch (RuntimeException exception) {
			throw exception;
		} catch (InvocationTargetException exception) {
			throw new UnableToInjectException("Unable to inject Data Source for " + target, exception.getTargetException());
		} catch (Exception exception) {
			throw new UnableToInjectException("Unable to inject Data Source for " + target, exception);
		}
	}
	
/* Private Constructor */
	/** <p>Prevents the {@link Prestige} from being constructed.</p> */
	private Prestige() { }
	
	/** Caches the Model injects previously found to reduce reflection impact */
	private static final Map<Class<?>, Method> _MODEL_INJECTORS = newHashMap();
	/** Caches the Data Source injects previously found to reduce reflection impact */
	private static final Map<Class<?>, Method> _DATA_SOURCE_INJECTORS = newHashMap();
	/** Caches the Presentation Fragment injects previously found to reduce reflection impact */
	private static final Map<Class<?>, Method> _PRESENTATION_FRAGMENT_INJECTORS = newHashMap();
    /** Caches the Presentation injects previously found to reduce reflection impact */
    private static final Map<Class<?>, Method> _PRESENTATION_INJECTORS = newHashMap();
	/** Caches the Presentation Fragment attaches previously found to reduce reflection impact */
	private static final Map<Class<?>, Method> _ATTACHERS = newHashMap(); 
	/** Empty method */
	private static final Method _NO_OP = null;
	private static SegueController _segue_controller;
}
