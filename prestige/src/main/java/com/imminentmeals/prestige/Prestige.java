package com.imminentmeals.prestige;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.reflections.Reflections;

import android.app.Activity;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Controller.Default;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.Presentation.NoProtocol;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

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

		@SuppressWarnings("rawtypes")
		@Override
		public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
			System.out.println("Pass  #" + _passes);
			final Elements element_utilities = processingEnv.getElementUtils();
			final Types type_utilities = processingEnv.getTypeUtils();
			final Filer filer = processingEnv.getFiler();
			final Reflections reflections = new Reflections("");
			final TypeElement presentation = element_utilities.getTypeElement(Presentation.class.getCanonicalName());
			
			if (_passes++ > 0)
				return true;
			
			final Map<TypeElement, Class<?>> presentation_protocols = newHashMap();
			final Map<TypeElement, Class<? extends Activity>> presentation_implementations = processPresentations(
					environment, element_utilities, type_utilities, reflections, presentation_protocols);
			
			final Map<TypeElement, Map<String, Class>> presentation_controller_implementations = newHashMap();
			final Map<Class<?>, String> controller_package_modules = newHashMap();
			processControllers(environment, element_utilities, type_utilities, reflections,
					presentation_protocols, presentation_implementations,
					presentation_controller_implementations,
					controller_package_modules);
			
			// Generates the SegueController
			Writer writer = null;
			try {
				final JavaFileObject segue_controller = filer.createSourceFile("com.imminentmeals.prestige.SegueController", 
						(Element) null);
		        writer = segue_controller.openWriter();
		        writer.flush();
		        final Set<TypeElement> imports = Sets.union(presentation_implementations.keySet(), 
        				presentation_controller_implementations.keySet()); 
		        final Set<TypeElement> controller_injections = presentation_controller_implementations.keySet();
		        final Collection<PresentationControllerBinding> controllers = transform(
		        		presentation_controller_implementations.keySet(),
		        		new Function<TypeElement, PresentationControllerBinding>() {

							@Override
							public PresentationControllerBinding apply(@Nullable TypeElement controller) {
								// FIXME: finish implementation
								return new PresentationControllerBinding(controller, controller);
							}
						});
		        final Map<String, Object> data_model = ImmutableMap.of("imports", imports, "controllers", (Object) controllers);
		        freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);	        
		        final Configuration template_configuration = new Configuration();
		        template_configuration.setClassForTemplateLoading(getClass(), "/templates/");
		        template_configuration.getTemplate("SegueControllerTemplate.ftl").process(data_model, writer);
		        writer.close();
		        final JavaFileObject model_view_controller_module = filer.createSourceFile(
		        		"com.imminentmeals.prestige.ModelViewControllerModule", 
						(Element) null);
		        writer = model_view_controller_module.openWriter();
		       /* writer.write("package com.imminentmeals.prestige;\nimport dagger.Module;\n@Module()public class ModelViewControllerModule { }\n");
		        writer.flush();*/
		        template_configuration.getTemplate("ModelViewControllerModule.ftl").process(data_model, writer);
			} catch (IOException exception) {
				processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
			} catch (TemplateException exception) {
				processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
			} catch (ClassNotFoundException exception) {
				processingEnv.getMessager().printMessage(ERROR, "Prestige: " + exception.getMessage());
			} finally {
				if (writer != null)
					try {
						writer.close();
			        } catch (Exception _) { }
			}
		     
			return true;
		}

		/**
		 * <p>Processes the Controllers.</p>
		 * @param environment
		 * @param element_utilities
		 * @param type_utilities
		 * @param controller_contract_type
		 * @param reflections
		 * @param presentation_protocols
		 * @param presentation_implementations
		 * @param presentation_controller_implementations
		 * @param controller_package_modules
		 */
		@SuppressWarnings("rawtypes")
		private void processControllers(RoundEnvironment environment, Elements element_utilities, 
				Types type_utilities, Reflections reflections, Map<TypeElement, Class<?>> presentation_protocols,
				Map<TypeElement, Class<? extends Activity>> presentation_implementations,
				Map<TypeElement, Map<String, Class>> presentation_controller_implementations,
				Map<Class<?>, String> controller_package_modules) {
			final TypeMirror controller_contract_type = element_utilities.getTypeElement(
					ControllerContract.class.getCanonicalName()).asType();
			final TypeMirror default_presentation = element_utilities.getTypeElement(Default.class.getCanonicalName()).asType();
			
			for (Element element : environment.getElementsAnnotatedWith(Controller.class)) {
				// Verifies that the target type is an interface
				/*if (element.getKind() != INTERFACE) {
					error(element, "@Controller annotations may only be specified on interfaces (%s).",
						  element);
					// Skips the current element
					continue;
				} */
				
				// Verifies that the interface's visibility isn't private
				/*if (element.getModifiers().contains(PRIVATE)) {
					error(element, "@Presentation interfaces may not be private (%s).",
						  element);
					// Skips the current element
					continue;
				}*/
				
				// Verifies that the interface extends the ControllerContract interface
				/*if (!type_utilities.isSubtype(element.asType(), controller_contract_type)) {
					error(element, "Controller interface must extend the ControllerContract (%s).",
			              element); 
			          // Skips the current element
			          continue;
				}*/
				
				// Assembles information on the Controller
				final Map<TypeElement, Class> presentation_controllers = newHashMap();
				final Controller controller_annotation = element.getAnnotation(Controller.class);
				TypeMirror presentation = null;
				try {
					controller_annotation.presentation();
				} catch (MirroredTypeException exception) {
					presentation = exception.getTypeMirror(); 
				}
				
				if (type_utilities.isSameType(presentation, default_presentation)) {
					// FIXME: implementation of code my naming convention is potentially insufficiently robust
					for (TypeElement presentation_interface : presentation_protocols.keySet()) {
						if (presentation_interface.getSimpleName().contentEquals(
								element.getSimpleName().toString().replaceAll("Controller", "Presentation"))) {
							presentation = presentation_interface.asType();
							break;
						}
					}
				}
				
				// Verifies a Presentation was found for the Controller
				/*if (presentation == Default.class || presentation_protocols.containsKey(element_utilities.getTypeElement(
						presentation.getCanonicalName()))) {
					error(element, "Expected to find a @Presentation-annotated interface named %s for @Controller (%s).",
							  element.getSimpleName().toString().replaceAll("Controller", "") + "Presentation",
							  element);
					// Skips the current element
					continue;
				}*/
					
				// Verifies that the Controller implements the Presentation's Protocol, if one is required
				final Class<?> protocol = presentation_protocols.get((TypeElement) type_utilities.asElement(presentation));
				/*if (!(protocol == null || type_utilities.isSubtype(element.asType(), 
						element_utilities.getTypeElement(protocol.getCanonicalName()).asType()))) {
					error(element, "@Controller is required to implement Protocol %s by its Presentation (%s).",
						  protocol.getCanonicalName(), element);
					// Skips the current element
					continue;
				}*/
				
				// Adds Controller to list
				presentation_controllers.put((TypeElement) element, (Class) presentation_implementations.get(presentation));
				
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
				        /*if (annotation == null) {
				          error(element, "Controller implementing classes must have @ControllerImplementation " +
				          		" or one of its nickname annotations (%s).",
				                controller_element); 
				          // Skips the current element
				          continue;
				        }*/
				        
				        // Assembles information on the Controller implementation
				        final String implementation_type = annotation.value();
				        
				        // Verifies the Controller implementation is defined only once
				        /*if (controller_implementations.containsKey(implementation_type)) {
				        	error(element, "%s-type Controller implemented multiple times (%s, %s).",
				        		  implementation_type, controller_implementation, 
				        		  controller_implementations.get(implementation_type));
				        	// Skips the current element
				        	continue;
				        }*/
				        
				        // Adds the Controller implementation to list
				        controller_implementations.put(implementation_type, controller_implementation);
				        controller_package_modules.put(controller_implementation, 
				        		element_utilities.getPackageOf(controller_element).getQualifiedName().toString());
					}
				} catch (ClassNotFoundException exception) {
					/*error(element, "Expected to find Class %s.",
						  ((TypeElement) element).getQualifiedName());
					// Skips the current element
					continue;*/
				}
				
				// Adds the Controller implementations to the list of all Controller implementations
				presentation_controller_implementations.put((TypeElement) element, controller_implementations);
			}
		}

		/**
		 * <p>Processes the Presentations.</p>
		 * @param environment
		 * @param element_utilities
		 * @param type_utilities
		 * @param activity_type
		 * @param reflections
		 * @param presentation_protocols
		 * @return
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private Map<TypeElement, Class<? extends Activity>> processPresentations(
				RoundEnvironment environment, Elements element_utilities, Types type_utilities,
				Reflections reflections, Map<TypeElement, Class<?>> presentation_protocols) {
			final TypeMirror activity_type = element_utilities.getTypeElement("android.app.Activity").asType();
			
			for (Element element : environment.getElementsAnnotatedWith(Presentation.class)) {
		        // Verifies that the target type is an interface
				/*if (element.getKind() != INTERFACE) {
					error(element, "@Presentation annotations may only be specified on interfaces (%s).",
						  element);
					// Skips the current element
					continue;
				}*/

				// Verifies that the interface's visibility isn't private
				/*if (element.getModifiers().contains(PRIVATE)) {
					error(element, "@Presentation interfaces must not be private (%s).",
						  element);
					// Skips the current element
					continue;
				}*/

				// Assembles information on the Presentation
				final TypeMirror no_protocol = element_utilities.getTypeElement(NoProtocol.class.getCanonicalName()).asType();
				final Presentation presentation_annotation = element.getAnnotation(Presentation.class);
				TypeMirror protocol = null;
				try {
					presentation_annotation.protocol();
				} catch (MirroredTypeException exception) {
					protocol = exception.getTypeMirror(); 
				}

				// Verifies that the Protocol is an Interface
				/*if (protocol != NoProtocol.class && !protocol.getClass().isInterface()) {
					error(element, "@Presentation Protocol must be an interface (%s).",
						  protocol.getClass().getCanonicalName());
					// Skips the current element
					continue;
				}*/
				
				// Verifies that the Protocol visibility isn't private
				final Element protocol_element = type_utilities.asElement(protocol);
				/*if (protocol != NoProtocol.class && protocol_element.getModifiers().contains(PRIVATE)) {
					error(element, "@Presentation Protocol must not be private (%s).",
							protocol_element);
					// Skips the current element
					continue;
				}*/

		        // Verifies the Presentation isn't already defined
		        /*if (presentation_protocols.containsKey(element)) {
		        	error(element, "@Presentation defined for the same interface multiple times (%s).",
		        		  element);
		        	// Skips the current element
		        	continue;
		        }*/
		        
		        // Adds Presentation to list
		        try {
					presentation_protocols.put((TypeElement) element, 
							type_utilities.isSameType(protocol, no_protocol)
								? null 
								: Class.forName(((TypeElement) type_utilities.asElement(protocol)).getQualifiedName().toString()));
				} catch (ClassNotFoundException exception) {
					error(protocol_element, exception.getMessage());
					continue;
				} 
			}
			
			final Map<TypeElement, Class<? extends Activity>> presentation_implementations = newHashMap();
			final Map<String, List<TypeElement>> package_modules = newHashMap();
			// Processes the Presentation implementations
			for (TypeElement presentation : presentation_protocols.keySet()) {
				for (Class<?> presentation_implementation : reflections.getSubTypesOf(presentation.asType().getClass())) {
					final TypeElement element = element_utilities.getTypeElement(
							presentation_implementation.getCanonicalName());
					
					// Verifies that the Presentation implementation extends from Activity
			        /*if (!type_utilities.isSubtype(element.asType(), activity_type)) {
			          error(element, "Presentation implementing classes must extend from Activity (%s).",
			                element); 
			          // Skips the current element
			          continue;
			        }*/
			        
			        // Verifies the Presentation isn't already implemented
			        /*if (presentation_implementations.containsKey(presentation)) {
			        	error(element, "Presentation implemented multiple times (%s, %s).",
			        		  presentation_implementation, presentation_implementations.get(presentation));
			        	// Skips the current element
			        	continue;
			        }*/
			        
			        // Adds the Presentation implementation
			        presentation_implementations.put(presentation, (Class<? extends Activity>) presentation_implementation);
			        final String container = element_utilities.getPackageOf(element).getQualifiedName().toString();
			        if (package_modules.containsKey(container))
		        		package_modules.get(container).add(element);
		        	else
		        		package_modules.put(container, newArrayList(element));     		
				}
			}
			return presentation_implementations;
		}
		
		private void error(Element element, String message, Object... arguments) {
			processingEnv.getMessager().printMessage(ERROR,
					String.format(message, arguments), element);
		}
		
		@SuppressWarnings("serial")
		private static class PresentationControllerBinding extends SimpleHash {
			
			public PresentationControllerBinding(TypeElement class_name, TypeElement presentation_implementation) {
				put(_CLASS_NAME, class_name.getSimpleName().toString());
				// TODO: use correct naming convention
				put(_VARIABLE_NAME, class_name.getSimpleName().toString());
				put(_PRESENTATION_IMPLEMENTATION, presentation_implementation.getSimpleName().toString());
			}
			
			private static final String _CLASS_NAME = "className";
			private static final String _VARIABLE_NAME = "variableName";
			private static final String _PRESENTATION_IMPLEMENTATION = "presentationImplementation";
		}
		
		/** Counts the number of passes The Annotation Processor has performed */
		private int _passes = 0;
	}
	
/* Private Constructor */
	/** <p>Prevents the {@link Prestige} from being constructed.</p> */
	private Prestige() { }
}
