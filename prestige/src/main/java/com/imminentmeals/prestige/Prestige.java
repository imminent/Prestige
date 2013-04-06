package com.imminentmeals.prestige;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter.DEFAULT;

import org.reflections.Reflections;

import android.app.Activity;
import android.util.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.Presentation;

/**
 *
 * @author Dandre Allison
 */
public final class Prestige {

	@SupportedAnnotationTypes({ "com.imminentmeals.prestige.annotations.Presentation",
		                        "com.imminentmeals.prestige.annotations.Controller",
		                        "com.imminentmeals.prestige.annotations.ControllerImplementation" })
	public static class AnnotationProcessor extends AbstractProcessor {

		@Override
		public SourceVersion getSupportedSourceVersion() {
			return SourceVersion.latestSupported();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public boolean process(Set<? extends TypeElement> elements, RoundEnvironment environment) {
			final Elements element_utilities = processingEnv.getElementUtils();
			final Types type_utilities = processingEnv.getTypeUtils();
			final Filer filer = processingEnv.getFiler();
			final TypeMirror activity_type = element_utilities.getTypeElement("android.app.Activity").asType();
			final TypeMirror controller_contract_type = element_utilities.getTypeElement(
					ControllerContract.class.getCanonicalName()).asType();
			final Reflections reflections = new Reflections("");
			final Map<TypeElement, Class<?>> presentation_protocols = newHashMap();
			
			// Processes the Presentations
			for (Element element : environment.getElementsAnnotatedWith(Presentation.class)) {
		        // Verifies that the target type is an interface
				if (element.getKind() != INTERFACE) {
					error(element, "@Presentation annotations may only be specified on interfaces (%s).",
						  element);
					// Skips the current element
					continue;
				}

				// Verifies that the interface's visibility isn't private
				if (element.getModifiers().contains(PRIVATE)) {
					error(element, "@Presentation interfaces must not be private (%s).",
						  element);
					// Skips the current element
					continue;
				}

				// Assembles information on the Presentation
				final Class<?> protocol = element.getAnnotation(
						Presentation.class).protocol();

				// Verifies that the Protocol is an Interface
				if (protocol != DEFAULT.class && !protocol.getClass().isInterface()) {
					error(element, "@Presentation Protocol must be an interface (%s).",
						  protocol.getClass().getName());
					// Skips the current element
					continue;
				}
				
				// Verifies that the Protocol visibility isn't private
				final Element protocol_element = element_utilities.getTypeElement(protocol.getCanonicalName());
				if (protocol != DEFAULT.class && protocol_element.getModifiers().contains(PRIVATE)) {
					error(element, "@Presentation Protocol must not be private (%s).",
							protocol_element);
					// Skips the current element
					continue;
				}

		        // Verifies the Presentation isn't already defined
		        if (presentation_protocols.containsKey(element)) {
		        	error(element, "@Presentation defined for the same interface multiple times (%s).",
		        		  element);
		        	// Skips the current element
		        	continue;
		        }
		        
		        // Adds Presentation to list
		        presentation_protocols.put((TypeElement) element, protocol == DEFAULT.class? null : protocol); 
			}
			
			final Map<TypeElement, Class<? extends Activity>> presentation_implementations = newHashMap();
			final Map<String, List<TypeElement>> package_modules = newHashMap();
			// Processes the Presentation implementations
			for (TypeElement presentation : presentation_protocols.keySet()) {
				for (Class<?> presentation_implementation : reflections.getSubTypesOf(presentation.asType().getClass())) {
					final TypeElement element = element_utilities.getTypeElement(
							presentation_implementation.getCanonicalName());
					
					// Verifies that the Presentation implementation extends from Activity
			        if (!type_utilities.isSubtype(element.asType(), activity_type)) {
			          error(element, "Presentation implementing classes must extend from Activity (%s).",
			                element); 
			          // Skips the current element
			          continue;
			        }
			        
			        // Verifies the Presentation isn't already implemented
			        if (presentation_implementations.containsKey(presentation)) {
			        	error(element, "Presentation implemented multiple times (%s, %s).",
			        		  presentation_implementation, presentation_implementations.get(presentation));
			        	// Skips the current element
			        	continue;
			        }
			        
			        // Adds the Presentation implementation
			        presentation_implementations.put(presentation, (Class<? extends Activity>) presentation_implementation);
			        final String container = element_utilities.getPackageOf(element).getQualifiedName().toString();
			        if (package_modules.containsKey(container))
		        		package_modules.get(container).add(element);
		        	else
		        		package_modules.put(container, newArrayList(element));     		
				}
			}
			
			final Map<TypeElement, Map<String, Class>> presentation_controller_implementations = newHashMap();
			final Map<Class<?>, String> controller_package_modules = newHashMap();
			// Processes the Controllers
			for (Element element : environment.getElementsAnnotatedWith(Controller.class)) {
				// Verifies that the target type is an interface
				if (element.getKind() != INTERFACE) {
					error(element, "@Controller annotations may only be specified on interfaces (%s).",
						  element);
					// Skips the current element
					continue;
				} 
				
				// Verifies that the interface's visibility isn't private
				if (element.getModifiers().contains(PRIVATE)) {
					error(element, "@Presentation interfaces may not be private (%s).",
						  element);
					// Skips the current element
					continue;
				}
				
				// Verifies that the interface extends the ControllerContract interface
				if (!type_utilities.isSubtype(element.asType(), controller_contract_type)) {
					error(element, "Controller interface must extend the ControllerContract (%s).",
			              element); 
			          // Skips the current element
			          continue;
				}
				
				// Assembles information on the Controller
				final List<Pair<TypeElement, Class>> presentation_controllers = newArrayList();
				Class<?> presentation = element.getAnnotation(Controller.class).presentation();
				if (presentation == DEFAULT.class) {
					// FIXME: implementation of code my naming convention is potentially insufficiently robust
					for (TypeElement presentation_interface : presentation_protocols.keySet()) {
						if (presentation_interface.getSimpleName().contentEquals(
								element.getSimpleName().toString().replaceAll("Controller", "Presentation"))) {
							try {
								presentation = Class.forName(presentation_interface.getQualifiedName().toString());
							} catch (ClassNotFoundException exception) {
								error(element, "Expected to find a @Presentation-annotated interface named %s for @Controller (%s).",
									  element.getSimpleName().toString().replaceAll("Controller", "") + "Presentation",
									  element);
								// Skips the current element
								continue;
							}
							break;
						}
					}
				}
				
				// Verifies a Presentation was found for the Controller
				if (presentation == DEFAULT.class || presentation_protocols.containsKey(element_utilities.getTypeElement(
						presentation.getCanonicalName()))) {
					error(element, "Expected to find a @Presentation-annotated interface named %s for @Controller (%s).",
							  element.getSimpleName().toString().replaceAll("Controller", "") + "Presentation",
							  element);
					// Skips the current element
					continue;
				}
					
				// Verifies that the Controller implements the Presentation's Protocol, if one is required
				final Class<?> protocol = presentation_protocols.get(element_utilities.getTypeElement(
						presentation.getCanonicalName()));
				if (!(protocol == null || type_utilities.isSubtype(element.asType(), 
						element_utilities.getTypeElement(protocol.getCanonicalName()).asType()))) {
					error(element, "@Controller is required to implement Protocol %s by its Presentation (%s).",
						  protocol.getCanonicalName(), element);
					// Skips the current element
					continue;
				}
				
				// Adds Controller to list
				presentation_controllers.add(Pair.create((TypeElement) element, 
						                                 (Class) presentation_implementations.get(presentation)));
				
				// Processes Controller implementations
				final Map<String, Class> controller_implementations = newHashMap();
				try {
					for (Class<?> controller_implementation : reflections.getSubTypesOf(Class.forName(
							((TypeElement) element).getQualifiedName().toString()))) {
						final TypeElement controller_element = element_utilities.getTypeElement(
								controller_implementation.getCanonicalName());
						
						// Verifies that the Controller implementation has a ControllerImplementation
						final ControllerImplementation annotation = 
								controller_implementation.getAnnotation(ControllerImplementation.class);
				        if (annotation == null) {
				          error(element, "Controller implementing classes must have @ControllerImplementation " +
				          		" or one of its nickname annotations (%s).",
				                controller_element); 
				          // Skips the current element
				          continue;
				        }
				        
				        // Assembles information on the Controller implementation
				        final String implementation_type = annotation.value();
				        
				        // Verifies the Controller implementation is defined only once
				        if (controller_implementations.containsKey(implementation_type)) {
				        	error(element, "%s-type Controller implemented multiple times (%s, %s).",
				        		  implementation_type, controller_implementation, 
				        		  controller_implementations.get(implementation_type));
				        	// Skips the current element
				        	continue;
				        }
				        
				        // Adds the Controller implementation to list
				        controller_implementations.put(implementation_type, controller_implementation);
				        controller_package_modules.put(controller_implementation, 
				        		element_utilities.getPackageOf(controller_element).getQualifiedName().toString());
					}
				} catch (ClassNotFoundException exception) {
					error(element, "Expected to find Class %s.",
						  ((TypeElement) element).getQualifiedName());
					// Skips the current element
					continue;
				}
				
				// Adds the Controller implementations to the list of all Controller implementations
				presentation_controller_implementations.put((TypeElement) element, controller_implementations);
			}
			
			// Generates the SequeController
			try {
				final JavaFileObject source = filer.createSourceFile("com.imminentmeals.prestige.SequeController", 
						(Element) null);
		        final Writer writer = source.openWriter();
		        writer.flush();
		        writer.write(String.format(_SEGUE_CONTROLLER, 
		        		transform(Sets.union(presentation_implementations.keySet(), 
		        				presentation_controller_implementations.keySet()),  
		        				new Function<TypeElement, String>() {
									@Override
									public String apply(@Nullable TypeElement element) {
										return String.format(_IMPORT, element.getQualifiedName());
									}
		        				}),
		        		transform(presentation_controller_implementations.keySet(),
		        				new Function<TypeElement, String>() {
									@Override
									public String apply(@Nullable TypeElement element) {
										return String.format(_CONTROLLER_INJECTION, element.getSimpleName(), element.getSimpleName());
									}	
								}),
						String.format(_PRESENTATION_TO_CONTROLLER, 
								transform(presentation_implementations.keySet(), 
										new Function<TypeElement, String>() {
											@Override
											public String apply(@Nullable TypeElement element) {
												return element.getSimpleName().toString();
											}
										}),
								transform(presentation_controller_implementations.keySet(), 
										new Function<TypeElement, String>() {
											@Override
											public String apply(@Nullable TypeElement element) {
												return element.getSimpleName().toString();
											}
										})
								)
		        		));
		        writer.close();
			} catch (IOException exception) {
				error(element_utilities.getTypeElement("SequeController"), "Unable to write SequeController: %s",
					  exception.getMessage());
			}
		      
			return true;
		}
		
		private void error(Element element, String message, Object... arguments) {
			processingEnv.getMessager().printMessage(ERROR,
					String.format(message, arguments), element);
		}
	}
	
/* Private Constructor */
	/** <p>Prevents the {@link Prestige} from being constructed.</p> */
	private Prestige() { }
	
	private static final String _IMPORT = "import %s;\n";
	private static final String _CONTROLLER_INJECTION = "\t@Inject /* package */Provider<%s> %s;\n";
	private static final String _PRESENTATION_TO_CONTROLLER = "\t\t\t.put(%s, %s)\n";
	private static final String _SEGUE_CONTROLLER = ""
			+ "// Generated code from Prestige. Do not modify!\n"
			+ "package com.imminentmeals.prestige;\n\n"
			+ "import static com.google.common.collect.Maps.newHashMap;\n\n"
			+ "import java.util.HashMap;\n\n"
			+ "import javax.inject.Inject;\n"
			+ "import javax.inject.Named;\n"
			+ "import javax.inject.Provider;\n\n"
			+ "import android.app.Activity;\n"
			+ "import android.app.Application.ActivityLifecycleCallbacks;\n"
			+ "import android.app.Fragment;\n"
			+ "import android.os.Bundle;\n\n"
			+ "import com.google.common.collect.ImmutableMap;\n"
			+ "import com.imminentmeals.prestige.ControllerContract;\n"
			+ "import com.squareup.otto.Bus;\n"
			// Dynamic imports
			+ "%s\n"
			+ "import dagger.Module;\n"
			+ "import dagger.ObjectGraph;\n\n"
			+ "public class SegueController implements ActivityLifecycleCallbacks {\n"
			+   "\t@Inject @Named(Controller.BUS)/* package */Bus controller_bus;\n"
			// Controller Injections 
			+   "%s\n"
			+   "\tpublic SegueController() {\n"
			+     "\t\tfinal ObjectGraph object_graph = ObjectGraph.create(new ModeViewControllerModule());\n"
			+     "\t\tobject_graph.inject(this);\n\n"
			+     "\t\t_presentation_controllers = new ImmutableMap.Builder<Class<? extends Activity, Provider<?>>()\n"
			// Presentation -> injected Controller 
			+       "%s"
			+       "\t\t\t.build();\n"
			+     "\t\t_controllers = newHashMap();\n"
			+   "\t}\n\n"
			+ "/* Activity Lifecycle Callbacks */\n"
			+   "\t@Override\n"
			+   "\tpublic void onActivityCreated(Activity activity, Bundle icicle) {\n"
			+     "\t\tif (!_presentation_controllers.containsKey(activity.getClass())) return;\n\n"
			+     "\t\tfinal ControllerContract controller = _presentation_controllers.get(activity.getClass());\n"
			+     "\t\tcontroller.attachPresentation(activity);\n"
			+     "\t\t_controllers.put(activity.getClass(), controller);\n"
			+   "\t}\n\n"
			+   "\t@Override\n"
	        +   "\tpublic void onActivityStarted(Activity activity) { }\n\n"
			+   "\t@Override\n"
			+   "\tpublic void onActivityResumed(Activity activity) { }\n\n"
			+   "\t@Override\n"
			+   "\tpublic void onActivityPaused(Activity activity) { }\n\n"
			+   "\t@Override\n"
			+   "\tpublic void onActivityStopped(Activity activity) { }\n\n"
			+   "\t@Override\n"
			+   "\tpublic void onActivityDestroyed(Activity activity) {\n"
			+     "\t\t_controllers.remove(activity.getClass());\n"
			+   "\t}\n\n"
			+   "\t@Override\n"
			+   "\tpublic void onActivitySaveInstanceState(Activity activity, Bundle outState) { }\n\n"
			+ "/* Exposed Controller API */\n"
			+   "\t@SuppressWarnings(\"unchecked\")\n"
			+   "\tpublic static <T> T controller(Activity presentation) {\n"
		    +     "\t\treturn (T) segueController(presentation)._controllers.get(presentation.getClass());\n"
			+   "}\n\n"
			+   "\tpublic static <T> T controller(Fragment presentation_fragment) {\n"
			+     "\t\treturn controller(presentation(presentation_fragment));\n"
			+   "\t}\n\n"
			+   "\tpublic static void sendMessage(Activity presentation, Object message) {\n"
			+     "\t\tsegueController(presentation).controller_bus.post(message);\n"
			+   "\t}\n\n"
			+   "\tpublic static void sendMessage(Fragment presentation_fragment, Object message) {\n"
			+     "\t\tsendMessage(presentation(presenation_fragment), message);\n"
			+   "\t}\n\n"
			+ "/* Private Helpers */\n"
			+   "\t@SuppressWarnings(\"unchecked\")\n"
	        +   "\tprivate static SegueController segueController(Activity presentation) {\n"
	        +	  "\t\treturn ((SegueControllerApplication) presentation.getApplication()).segueController();\n"
	        +   "\t}\n\n"
			+   "\t@SuppressWarnings(\"unchecked\")\n"
			+   "\tprivate static Activity presentation(Fragment presentation_fragment) {\n"
			+	  "\t\tfinal Activity presentation;\n"
			+     "\t\tif (presentation == null)\n"
			+       "\t\t\tthrow new IllegalStateException(\"Fragment \" + presentation_fragment.getClass().getName()\n"
			+         "\t\t\t\t+ \" isn't attached to an Activity\");\n\n"
			+     "\t\tif (presentation.getClass().getAnnotation(Presentation.class) == null) {\n"
			+       "\t\t\tthrow new IllegalArgumentException(\"Fragment \" + presentation_fragment.getClass().getName()\n"
			+	      "\t\t\t\t+ \" isn't attached to a Presentation\");\n"
			+     "\t\t}\n\n"
			+     "\t\treturn presentation;\n"
			+   "\t}\n\n"
	        +   "\t/** Provides the Controller implementation for the given Activity */"
			+   "\tprivate final ImmutableMap<Class<? extends Activity>, Provider<?>> _presentation_controllers;\n"
			+   "\t/** Maintains the set of currently used controllers */"
			+   "\tprivate final Map<Class<? extends Activity>, ?> _controllers;"
			+ "}\n";
}
