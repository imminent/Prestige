package com.imminentmeals.prestige;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Fragment;
import android.os.Bundle;
import com.imminentmeals.prestige.codegen.AnnotationProcessor;
import com.imminentmeals.prestige.codegen.Finder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import timber.log.Timber;

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;

/**
 * TODO: can I use interfaces to make this generally applicable to Java while working in an Android
 * environment?
 *
 * @author Dandre Allison
 */
@ParametersAreNonnullByDefault
public final class Prestige {

  /* package */static final String _TAG = "Prestige";
  private static final String _UNABLE_TO_INJECT_DATA_SOURCE_FOR =
      "Unable to inject Data Source for ";
  /** Caches the Model storers previously found to reduce reflection impact */
  private static final Map<Class<?>, Method> _MODEL_STORERS = newHashMap();
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
  private static final String _MESSAGE_SEGUE_CONTROLLER_MISSING =
      "Attempting to %s before Segue Controller was created " +
          "(Prestige.conjureSegueController(String)).";
  /** Empty method */
  private static final Method _NO_OP = null;
  private static SegueController _segue_controller;

  /** <p>Prevents the {@link Prestige} from being constructed.</p> */
  private Prestige() { }

  /**
   * <p>Injects a Data Source into the given {@link Activity}.</p>
   *
   * @param activity The target of the injection
   */
  public static void conjureController(Activity activity) {
    if (_segue_controller == null) {
      throw new IllegalStateException(
          format(_MESSAGE_SEGUE_CONTROLLER_MISSING, "create controller"));
    }
    // Creates Controller
    _segue_controller.createController(activity);
    injectDataSource(Finder.ACTIVITY, activity);
  }

  /**
   * <p>Injects the Presentation Fragments for the given display into the given Presentation.</p>
   *
   * @param activity The given Presentation
   * @param display The given display
   */
  public static void injectPresentationFragments(Activity activity, int display) {
    injectPresentationFragments(Finder.ACTIVITY, display, activity);
  }

  /**
   * <p>Injects a Data Source into the given {@link Fragment}.</p>
   *
   * @param fragment The target of the injection
   */
  public static void injectDataSource(Fragment fragment) {
    injectDataSource(Finder.FRAGMENT, fragment);
  }

  /**
   * <p>Sends the given message on the Controller Bus.</p>
   *
   * @param message The message to send
   */
  public static void sendMessage(Object message) {
    if (_segue_controller == null) {
      throw new IllegalStateException(format(_MESSAGE_SEGUE_CONTROLLER_MISSING, "send message"));
    }
    Timber.d("Sending message to controller: " + message);
    _segue_controller.sendMessage(message);
  }

  /**
   * <p>Creates the {@link SegueController} set for the given implementation scope. The
   * implementation scope specifies which version of implementations to use.</p>
   *
   * @param scope The implementation scope
   */
  public static void conjureSegueController(String scope) {
    try {
      final Class<?> segue_controller =
          Class.forName("com.imminentmeals.prestige._SegueController");
      _segue_controller =
          (SegueController) segue_controller.getConstructor(String.class).newInstance(scope);
    } catch (IllegalArgumentException | SecurityException | InstantiationException
        | IllegalAccessException | NoSuchMethodException exception) {
      throw new UnableToConjureSegueControllerException("Error while conjuring Segue Controller"
          , exception);
    } catch (InvocationTargetException exception) {
      throw new UnableToConjureSegueControllerException("Error while conjuring Segue Controller"
          , exception.getTargetException());
    } catch (ClassNotFoundException exception) {
      throw new UnableToConjureSegueControllerException(
          "Generated _SegueController cannot be found. Was Prestige annotation processor "
              + "executed? Was it removed by ProGuard?", exception);
    }
  }

/* Helpers */

  /**
   * <p>Destroys the constructed Controller for the given {@link Activity}.</p>
   *
   * @param activity The given Activity
   */
  public static void vanishController(Activity activity) {
    if (_segue_controller == null) {
      throw new IllegalStateException(
          format(_MESSAGE_SEGUE_CONTROLLER_MISSING, "destroy controller"));
    }
    _segue_controller.didDestroyActivity(activity);
  }

  public static void registerForControllerBus(Activity activity) {
    if (_segue_controller == null) {
      throw new IllegalStateException(
          format(_MESSAGE_SEGUE_CONTROLLER_MISSING, "register controller"));
    }
    _segue_controller.registerForControllerBus(activity);
  }

  public static void unregisterForControllerBus(Activity activity) {
    if (_segue_controller == null) {
      throw new IllegalStateException(
          format(_MESSAGE_SEGUE_CONTROLLER_MISSING, "unregister controller"));
    }
    _segue_controller.unregisterForControllerBus(activity);
  }

  /**
   * <p>Retrieves the {@link ActivityLifecycleCallbacks} that binds Prestige to the Activity
   * lifecycles so that it can conjure Controllers behind the curtains.</p>
   *
   * @return The Prestige callbacks
   */
  @TargetApi(ICE_CREAM_SANDWICH)
  public static ActivityLifecycleCallbacks activityLifecycleCallbacks() {
    // TODO: move to constant field
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
      public void onActivityStopped(Activity activity) {
        storeController(activity);
      }

      @Override
      public void onActivityStarted(Activity _) {
      }

      @Override
      public void onActivitySaveInstanceState(Activity _, Bundle __) {
      }
    };
  }

  /**
   * <p>Materializes Prestige for the given scope and binds it to the Activity lifecycles in the
   * given Application. This is equivalent to how you would use {@link
   * #conjureSegueController(String)} and {@link Prestige#activityLifecycleCallbacks()} in most
   * cases.</p>
   *
   * @param application The application using Prestige
   * @param scope The implementation scope
   */
  @TargetApi(ICE_CREAM_SANDWICH)
  public static void materialize(Application application, String scope) {
    application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks());
    conjureSegueController(scope);
  }

  /**
   * <p>Attaches the given Presentation Fragment to the Controller for the give Presentation.</p>
   * <p>It is recommended to do this in {@link Activity#onAttachFragment(Fragment)}.</p>
   */
  @TargetApi(HONEYCOMB)
  public static void attachPresentationFragment(Activity activity, Fragment fragment) {
    if (_segue_controller == null) {
      throw new IllegalStateException(
          format(_MESSAGE_SEGUE_CONTROLLER_MISSING, "attach Presentation Fragment"));
    }
    if (fragment.getTag() == null) {
      throw new IllegalArgumentException("Unable to inject Presentation Fragments without a tag: " +
          fragment.getClass().getCanonicalName());
    }
    _segue_controller.attachPresentationFragment(activity, fragment, fragment.getTag());
  }

/* Private Constructor */

  public static void storeController(Activity activity) {
    if (_segue_controller == null) {
      throw new IllegalStateException(
          format(_MESSAGE_SEGUE_CONTROLLER_MISSING, "store controller"));
    }
    _segue_controller.storeController(activity);
  }

  public static void injectModels(Object target) {
    final Class<?> target_class = target.getClass();
    injectModelsForClass(target, target_class);
    // TODO: generate ahead of time in generated method instead
    // Crawls up the hierarchy to find super class @InjectModels
    for (Class super_class = target_class.getSuperclass(); super_class != null;
        super_class = super_class.getSuperclass())
      injectModelsForClass(target, super_class);
  }

  /**
   * <p>Injects a Data Source into the given target.</p>
   *
   * @param finder The finder that specifies how to retrieve the Segue Controller from the target
   * @param target The target of the injection
   */
	/* package */
  static void injectDataSource(Finder finder, Object target) {
    final Class<?> target_class = target.getClass();
    try {
      final Method inject;
      if (!_DATA_SOURCE_INJECTORS.containsKey(target_class)) {
        final Class<?> injector =
            Class.forName(target_class.getName() + AnnotationProcessor.DATA_SOURCE_INJECTOR_SUFFIX);
        inject = injector.getMethod("injectDataSource", SegueController.class, Finder.class,
            target_class);
        _DATA_SOURCE_INJECTORS.put(target_class, inject);
      } else {
        inject = _DATA_SOURCE_INJECTORS.get(target_class);
      }
      // Allows for no-ops when there's nothing to inject
      if (inject != null) {
        inject.invoke(null, _segue_controller, finder, target);
      }
    } catch (ClassNotFoundException _) {
      // Allows injectDataSource to be called on targets without a data source
      _DATA_SOURCE_INJECTORS.put(target_class, _NO_OP);
    } catch (InvocationTargetException exception) {
      throw new UnableToInjectException(_UNABLE_TO_INJECT_DATA_SOURCE_FOR + target,
          exception.getTargetException());
    } catch (Exception exception) {
      throw new UnableToInjectException(_UNABLE_TO_INJECT_DATA_SOURCE_FOR + target, exception);
    }
  }

  /**
   * <p>Injects a Presentation into the given target.</p>
   *
   * @param target The target of the injection
   */
    /* package */
  static void attachPresentation(Object target, Object presentation) {
    final Class<?> target_class = target.getClass();
    try {
      final Method inject;
      if (!_PRESENTATION_INJECTORS.containsKey(target_class)) {
        final Class<?> injector = Class.forName(
            target_class.getName() + AnnotationProcessor.PRESENTATION_INJECTOR_SUFFIX);
        inject = injector.getMethod("attachPresentation", target_class, Object.class);
        _PRESENTATION_INJECTORS.put(target_class, inject);
      } else {
        inject = _PRESENTATION_INJECTORS.get(target_class);
      }
      // Allows for no-ops when there's nothing to inject
      if (inject != null) {
        inject.invoke(null, target, presentation);
      }
    } catch (ClassNotFoundException _) {
      // Allows attachPresentation to be called on targets without a presentation field
      _PRESENTATION_INJECTORS.put(target_class, _NO_OP);
    } catch (InvocationTargetException exception) {
      throw new UnableToInjectException("Unable to inject Presentation for " + target,
          exception.getTargetException());
    } catch (Exception exception) {
      throw new UnableToInjectException("Unable to inject Presentation for " + target, exception);
    }
  }

  /**
   * <p>Injects a Presentation Fragment into the given target.</p>
   *
   * @param target The target of the injection
   */
	/* package */
  static void injectPresentationFragments(Finder finder, int display, Object target) {
    final Class<?> target_class = target.getClass();
    try {
      final Method inject;
      if (!_PRESENTATION_FRAGMENT_INJECTORS.containsKey(target_class)) {
        final Class<?> injector = Class.forName(
            target_class.getName() + AnnotationProcessor.PRESENTATION_FRAGMENT_INJECTOR_SUFFIX);
        inject = injector.getMethod("injectPresentationFragments", Finder.class, int.class,
            target_class);
        _PRESENTATION_FRAGMENT_INJECTORS.put(target_class, inject);
      } else {
        inject = _PRESENTATION_FRAGMENT_INJECTORS.get(target_class);
      }
      // Allows for no-ops when there's nothing to inject
      if (inject != null) {
        inject.invoke(null, finder, display, target);
      }
    } catch (ClassNotFoundException _) {
      // Allows injectDataSource to be called on targets without a data source
      _PRESENTATION_FRAGMENT_INJECTORS.put(target_class, _NO_OP);
    } catch (InvocationTargetException exception) {
      throw new UnableToInjectException("Unable to inject Presentation Fragment for " + target,
          exception.getTargetException());
    } catch (Exception exception) {
      throw new UnableToInjectException("Unable to inject Presentation Fragment for " + target,
          exception);
    }
  }

  /* package */
  static void attachPresentationFragment(Object target, Object presentation_fragment,
      String tag) {
    final Class<?> target_class = target.getClass();
    try {
      final Method attach;
      if (!_ATTACHERS.containsKey(target_class)) {
        final Class<?> injector = Class.forName(
            target_class.getName() + AnnotationProcessor.PRESENTATION_FRAGMENT_INJECTOR_SUFFIX);
        attach = injector.getMethod("attachPresentationFragment", target_class, Object.class,
            String.class);
        _ATTACHERS.put(target_class, attach);
      } else {
        attach = _ATTACHERS.get(target_class);
      }
      // Allows for no-ops when there's nothing to inject
      if (attach != null) {
        attach.invoke(null, target, presentation_fragment, tag);
      }
    } catch (ClassNotFoundException _) {
      // Allows injectDataSource to be called on targets without a data source
      _ATTACHERS.put(target_class, _NO_OP);
    } catch (InvocationTargetException exception) {
      throw new UnableToInjectException(_UNABLE_TO_INJECT_DATA_SOURCE_FOR + target,
          exception.getTargetException());
    } catch (Exception exception) {
      throw new UnableToInjectException(_UNABLE_TO_INJECT_DATA_SOURCE_FOR + target, exception);
    }
  }

  @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    /* package */ static <T> void store(T source) throws IOException {
    final Class<?> source_class = source.getClass();
    try {
      final Method inject;
      if (!_MODEL_STORERS.containsKey(source_class)) {
        final Class<?> injector =
            Class.forName(source_class.getName() + AnnotationProcessor.MODEL_INJECTOR_SUFFIX);
        inject = injector.getMethod("storeModels", SegueController.class, source_class);
        _MODEL_STORERS.put(source_class, inject);
      } else {
        inject = _MODEL_STORERS.get(source_class);
      }
      // Allows for no-ops when there's nothing to store
      if (inject != null) {
        inject.invoke(null, _segue_controller, source);
      }
    } catch (ClassNotFoundException _) {
      // Allows store to be called on targets without injected Models
      _MODEL_STORERS.put(source_class, _NO_OP);
    } catch (InvocationTargetException exception) {
      throw new UnableToStoreException("Unable to store Models for " + source,
          exception.getTargetException());
    } catch (Exception exception) {
      throw new UnableToStoreException("Unable to store Models for " + source, exception);
    }
  }

/* Private Helpers */
  private static void injectModelsForClass(Object target, Class target_class) {
    try {
      final Method inject;
      if (!_MODEL_INJECTORS.containsKey(target_class)) {
        final Class<?> injector =
            Class.forName(target_class.getName() + AnnotationProcessor.MODEL_INJECTOR_SUFFIX);
        inject = injector.getMethod("injectModels", SegueController.class, target_class);
        _MODEL_INJECTORS.put(target_class, inject);
      } else {
        inject = _MODEL_INJECTORS.get(target_class);
      }
      // Allows for no-ops when there's nothing to inject
      if (inject != null) {
        inject.invoke(null, _segue_controller, target_class.cast(target));
      }
    } catch (ClassNotFoundException _) {
      // Allows injectModels to be called on targets without injected Models
      _MODEL_INJECTORS.put(target_class, _NO_OP);
    } catch (InvocationTargetException exception) {
      throw new UnableToInjectException("Unable to inject Models for " + target,
          exception.getTargetException());
    } catch (Exception exception) {
      throw new UnableToInjectException("Unable to inject Models for " + target, exception);
    }
  }

  /**
   * <p>Indicates an unexpected error occurred while attempting to inject a target.</p>
   *
   * @author Dandre Allison
   */
  @SuppressWarnings("serial")
  public static class UnableToInjectException extends RuntimeException {

    /**
     * <p>Creates an {@link UnableToInjectException}.</p>
     *
     * @param message The detailed message
     * @param cause The cause of the error
     */
	    /* package */UnableToInjectException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * <p>Indicates an unexpected error occurred while attempting to store a source.</p>
   *
   * @author Dandre Allison
   */
  @SuppressWarnings("serial")
  public static class UnableToStoreException extends RuntimeException {

    /**
     * <p>Creates an {@link UnableToStoreException}.</p>
     *
     * @param message The detailed message
     * @param cause The cause of the error
     */
	    /* package */UnableToStoreException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * <p>Indicates an unexpected error occurred while attempting to conjure the Segue Controller.</p>
   *
   * @author Dandre Allison
   */
  public static class UnableToConjureSegueControllerException extends RuntimeException {

    /**
     * <p>Creates an @{link UnableToConjureSegueControllerException}.</p>
     * @param message The detailed message
     * @param cause The cause of the error
     */
    /* package */UnableToConjureSegueControllerException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
