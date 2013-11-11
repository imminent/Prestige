package com.imminentmeals.prestige;

import android.app.Activity;

import com.google.common.collect.ImmutableMap;
import com.google.gson.InstanceCreator;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import dagger.Lazy;
import dagger.ObjectGraph;
import timber.log.Timber;

import static com.imminentmeals.prestige.Prestige._TAG;

/**
 *
 * @author Dandre Allison
 */
@ParametersAreNonnullByDefault
public abstract class SegueController {
    /** Bus over which Presentations communicate to their Controllers */
    @Inject
    @Named(com.imminentmeals.prestige.ControllerContract.BUS)
    /* package */Bus controller_bus;

/* Constructor */
    protected SegueController(String scope, Timber log) {
        this.scope = scope;
        this.log = log;
        _controllers = new HashMap<>();
        /* Dependency injection object graph */
        final ObjectGraph object_graph = createObjectGraph();
        object_graph.inject(this);
        _presentation_controllers = bindPresentationsToControllers();
        _model_implementations = provideModelImplementations();
    }

/* SegueController Contract */
    public abstract <T> void store(T object) throws IOException;

    @Nonnull protected abstract ObjectGraph createObjectGraph();

    @Nonnull protected abstract ImmutableMap<Class<?>, Provider> bindPresentationsToControllers();

    @Nonnull protected abstract ImmutableMap<Class<?>, Lazy> provideModelImplementations();

/* Public API */
    @SuppressWarnings("unchecked")
    @Nonnull public <T> T dataSource(Class<?> target) {
        log.tag(_TAG).d("Injecting " + _controllers.get(target) + " into " + target);
        return (T) _controllers.get(target);
    }

    public void sendMessage(Object message) {
        controller_bus.post(message);
    }

    public void createController(Activity activity) {
        final Class<?> activity_class = activity.getClass();
        if (!_presentation_controllers.containsKey(activity_class)) return;

        final Object controller = _presentation_controllers.get(activity_class).get();
        Prestige.injectModels(controller);
        Prestige.attachPresentation(controller, activity);
        _controllers.put(activity_class, controller);
    }

    public void didDestroyActivity(Activity activity) {
        final Class<?> activity_class = activity.getClass();
        if (!_presentation_controllers.containsKey(activity_class)) return;

        log.tag(_TAG).d("Vanishing " + _controllers.get(activity_class) + "(for " + activity + ")");
        _controllers.remove(activity_class);
    }

    @SuppressWarnings("unchecked")
    @Nonnull public <M, I extends M> I createModel(Class<M> model_interface) {
        return (I) _model_implementations.get(model_interface).get();
    }

    public void attachPresentationFragment(Activity activity, Object presentation_fragment, String tag) {
        final Object controller = _controllers.get(activity.getClass());
        if (controller != null) {
            log.tag(_TAG).d("Attaching " + presentation_fragment + " to " + controller + " (for " + activity + ")");
            Prestige.attachPresentationFragment(controller, presentation_fragment, tag);
        }
    }

    public void registerForControllerBus(Activity activity) {
        final Class<?> activity_class = activity.getClass();
        if (!_controllers.containsKey(activity_class)) return;

        log.tag(_TAG).d("Registering " + _controllers.get(activity_class) + " to receive messages (for " + activity + ")");
        controller_bus.register(_controllers.get(activity_class));
    }

    public void unregisterForControllerBus(Activity activity) {
        final Class<?> activity_class = activity.getClass();
        if (!_controllers.containsKey(activity_class)) return;

        log.tag(_TAG).d("Unregistering " + _controllers.get(activity_class) + " to receive messages (for " + activity + ")");
        controller_bus.unregister(_controllers.get(activity_class));
    }

    @Nonnull public Timber timber() {
        return log;
    }

    public void storeController(Activity activity) {
        final Class<?> activity_class = activity.getClass();
        if (!_controllers.containsKey(activity_class)) return;

        log.tag(_TAG).d("Storing controller " + _controllers.get(activity_class) + " (for " + activity + ")");
        try {
            Prestige.store(_controllers.get(activity_class));
        } catch (IOException exception) {
            log.tag(_TAG).d(exception, "Error storing controller" + _controllers.get(activity_class));
        }
    }

    @Nonnull public <M, I extends M> InstanceCreator<I> instanceCreator(final Class<M> type) {
        return new InstanceCreator<I>() {
            public I createInstance(Type _) {
                return createModel(type);
            }
        };
    }

    /**  The current scope */
    protected final String scope;
    /**  Log where messages are written */
    protected final Timber log;
  /** Provides the Controller implementation for the given Presentation Implementation */
    private final ImmutableMap<Class<?>, Provider> _presentation_controllers;
    /** Maintains the Controller references as they are being used */
    private final Map<Class<?>, Object> _controllers;
    /** Provides the Model implementation for the given Model interface */
    private final ImmutableMap<Class<?>, Lazy> _model_implementations;
}
