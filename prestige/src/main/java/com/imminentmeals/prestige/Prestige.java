package com.imminentmeals.prestige;

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

import com.google.common.collect.ImmutableMap;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Controller.Default;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.Presentation.NoProtocol;
import com.imminentmeals.prestige.annotations.PresentationImplementation;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;

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

		/**
		 * <p>Processes the source code.</p>
		 * @param _ Unused
		 * @param environment The round environment
		 * @return {@code true} indicating that the processed annotations have been completely handled
		 */
		@Override
		public boolean process(Set<? extends TypeElement> _, RoundEnvironment environment) {
			// Makes sure to only process once
			if (_passes++ > 0)
				return true;
			
			// Grabs the annotation processing utilities
			_element_utilities = processingEnv.getElementUtils();
			_type_utilities = processingEnv.getTypeUtils();
			
			// Initializes the data model subcomponents that will be used when generating the code
			final List<TypeElement> imports = newArrayList();
			final List<PresentationControllerBinding> presentation_controller_bindings = newArrayList();
			
			// Process the @Presentation annotations
			processPresentations(environment, imports);
			
			// Process the @Controller annotations and @ControllerImplementation annotations per @Controller annotation
			processControllers(environment, imports, presentation_controller_bindings);
			
			/*final Map<TypeElement, Class<?>> presentation_protocols = newHashMap();
			final Map<TypeElement, Class<? extends Activity>> presentation_implementations = processPresentations(
					environment, presentation_protocols);
			
			final Map<TypeElement, Map<String, Class>> presentation_controller_implementations = newHashMap();
			final Map<Class<?>, String> controller_package_modules = newHashMap();
			processControllers(environment,
					presentation_protocols, presentation_implementations,
					presentation_controller_implementations,
					controller_package_modules);*/
			
			// Creates the data model used to generate the code
			final Map<String, List<?>> data_model = ImmutableMap.of(
					"imports", imports, 
                    "controllers", presentation_controller_bindings);
			/*imports.addAll(Sets.union(presentation_implementations.keySet(), 
			presentation_controller_implementations.keySet())); 
    final Set<TypeElement> controller_injections = presentation_controller_implementations.keySet();
    final Collection<PresentationControllerBinding> controllers = transform(
    		presentation_controller_implementations.keySet(),
    		new Function<TypeElement, PresentationControllerBinding>() {

				@Override
				public PresentationControllerBinding apply(@Nullable TypeElement controller) {
					// FIXME: finish implementation
					return new PresentationControllerBinding(controller, controller);
				}
			});*/
			
			// Configures the templates generating the code        
	        final Configuration template_configuration = createTemplateConfiguration();
	        
			// Generates the code
			final Filer filer = processingEnv.getFiler();
			Writer writer = null;
			try {				
				// Generates the SegueController
				JavaFileObject source_code = filer.createSourceFile(_SEGUE_CONTROLLER_SOURCE, (Element) null);
		        writer = source_code.openWriter();
		        writer.flush();
		        template_configuration.getTemplate(_SEGUE_CONTROLLER_TEMPLATE).process(data_model, writer);
		        writer.close();
		        
		        // Generates the ModelViewControllerModule
		        source_code = filer.createSourceFile(_MODEL_VIEW_CONTROLLER_MODULE_SOURCE, (Element) null);
		        writer = source_code.openWriter();
		        template_configuration.getTemplate(_MODEL_VIEW_CONTROLLER_MODULE_TEMPLATE).process(data_model, writer);
			} catch (IOException exception) {
				processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
			} catch (TemplateException exception) {
				processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
			} finally {
				if (writer != null)
					try { writer.close(); } catch (Exception __) { }
			}
		     
			// Releases the annotation processing utilities
			_element_utilities = null;
			_type_utilities = null;
			
			return true;
		}

		/**
		 * <p>Processes the source code for the @Presentation annotation.</p>
		 * @param environment The round environment
		 * @param imports Will hold the list to place Presentation implementation classes
		 */
		private void processControllers(RoundEnvironment environment, List<TypeElement> imports,
				                        List<PresentationControllerBinding> presentation_controller_bindings) {
			// TODO Auto-generated method stub			
		}

		/**
		 * @param environment
		 * @param imports
		 */
		private void processPresentations(RoundEnvironment environment, List<TypeElement> imports) {
			final TypeMirror activity_type = _element_utilities.getTypeElement(Activity.class.getCanonicalName()).asType();
			final TypeMirror no_protocol = _element_utilities.getTypeElement(NoProtocol.class.getCanonicalName()).asType();
			
			for (Element element : environment.getElementsAnnotatedWith(Presentation.class)) {
				System.out.println("@Presentation is " + element);
				
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
				
				// Gathers @Presentation information
				final Presentation presentation_annotation = element.getAnnotation(Presentation.class);
				Element protocol = null;
				try {
					presentation_annotation.protocol();
				} catch (MirroredTypeException exception) {
					protocol = _type_utilities.asElement(exception.getTypeMirror());
				}
				
				System.out.println("\twith Protocol: " + protocol);
				
				// Verifies that the Protocol is an Interface
				if (protocol.getKind() != INTERFACE) {
					error(element, "@Presentation Protocol must be an interface (%s).",
						  protocol);
					// Skips the current element
					continue;
				}
				
				// Verifies that the Protocol visibility isn't private
				if (protocol.getModifiers().contains(PRIVATE)) {
					error(element, "@Presentation Protocol must not be private (%s).",
							protocol);
					// Skips the current element
					continue;
				}

				// Now that the @Presentation annotation has been verified and its data extracted find the its implementations				
				// TODO: very inefficient
				for (Element implementation_element : environment.getElementsAnnotatedWith(PresentationImplementation.class)) {
					// Makes sure to only deal with Presentation implementations for the current @Presentation
					if (!_type_utilities.isSubtype(implementation_element.asType(), element.asType()))
						continue;
					
					System.out.println("\twith an implementation of " + implementation_element);
					
					// Verifies that the Presentation implementation extends from Activity
			        if (!_type_utilities.isSubtype(implementation_element.asType(), activity_type)) {
			          error(element, "Presentation implementing classes must extend from Activity (%s).",
			                element); 
			          // Skips the current element
			          continue;
			        }
			        
			        // Adds the implementation to the list of imports
					imports.add((TypeElement) implementation_element);
				}
			}
		}
		
		/**
		 * <p>Retrieves a {@link Class} from the given {@link Element}.</p>
		 * @param element The given element
		 * @return The retrieved Class
		 */
		private Class<?> classFromElement(Element element) {
			System.out.println("Get Class for " + element);
			try {
				return Class.forName(_element_utilities.getBinaryName((TypeElement) element).toString());
			} catch (ClassNotFoundException exception) {
				throw new ClassFromElementException(exception);
			}
		}

		/**
		 * <p>Processes the Controllers.</p>
		 * @param environment
		 * @param controller_contract_type
		 * @param presentation_protocols
		 * @param presentation_implementations
		 * @param presentation_controller_implementations
		 * @param controller_package_modules
		 */
		@SuppressWarnings("rawtypes")
		private void processControllers(RoundEnvironment environment, Map<TypeElement, Class<?>> presentation_protocols,
				Map<TypeElement, Class<? extends Activity>> presentation_implementations,
				Map<TypeElement, Map<String, Class>> presentation_controller_implementations,
				Map<Class<?>, String> controller_package_modules) {
//			final TypeMirror controller_contract_type = _element_utilities.getTypeElement(
//					ControllerContract.class.getCanonicalName()).asType();
			final TypeMirror default_presentation = _element_utilities.getTypeElement(Default.class.getCanonicalName()).asType();
			
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
				
				if (_type_utilities.isSameType(presentation, default_presentation)) {
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
				final Class<?> protocol = presentation_protocols.get((TypeElement) _type_utilities.asElement(presentation));
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
					for (Class<?> controller_implementation : _reflections.getSubTypesOf(Class.forName(
							((TypeElement) element).getQualifiedName().toString()))) {
						final TypeElement controller_element = _element_utilities.getTypeElement(
								controller_implementation.getCanonicalName());
						
						// Verifies that the Controller implementation has a ControllerImplementation
						/*final ControllerImplementation annotation = 
								controller_implementation.getAnnotation(ControllerImplementation.class);
				        if (annotation == null) {
				          error(element, "Controller implementing classes must have @ControllerImplementation " +
				          		" or one of its nickname annotations (%s).",
				                controller_element); 
				          // Skips the current element
				          continue;
				        }*/
				        
				        // Assembles information on the Controller implementation
				        /*final String implementation_type = annotation.value();
				        
				        // Verifies the Controller implementation is defined only once
				        if (controller_implementations.containsKey(implementation_type)) {
				        	error(element, "%s-type Controller implemented multiple times (%s, %s).",
				        		  implementation_type, controller_implementation, 
				        		  controller_implementations.get(implementation_type));
				        	// Skips the current element
				        	continue;
				        }*/
				        
				        // Adds the Controller implementation to list
				        /*controller_implementations.put(implementation_type, controller_implementation);
				        controller_package_modules.put(controller_implementation, 
				        		_element_utilities.getPackageOf(controller_element).getQualifiedName().toString());*/
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
		 * @param activity_type
		 * @param presentation_protocols
		 * @return
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private Map<TypeElement, Class<? extends Activity>> processPresentations(
				RoundEnvironment environment, Map<TypeElement, Class<?>> presentation_protocols) {
			
			for (Element element : environment.getElementsAnnotatedWith(PresentationImplementation.class)) {
		        
		        // Adds Presentation to list
		        /*try {
					presentation_protocols.put((TypeElement) element, 
							_type_utilities.isSameType(protocol, no_protocol)
								? null 
								: Class.forName(((TypeElement) _type_utilities.asElement(protocol)).getQualifiedName().toString()));
				} catch (ClassNotFoundException exception) {
					error(protocol_element, exception.getMessage());
					continue;
				} */
			}
			
			final Map<TypeElement, Class<? extends Activity>> presentation_implementations = newHashMap();
			final Map<String, List<TypeElement>> package_modules = newHashMap();
			// Processes the Presentation implementations
			for (TypeElement presentation : presentation_protocols.keySet()) {
				for (Class<?> presentation_implementation : _reflections.getSubTypesOf(presentation.asType().getClass())) {
					final TypeElement element = _element_utilities.getTypeElement(
							presentation_implementation.getCanonicalName());
					
					
			        
			        // Verifies the Presentation isn't already implemented
			        /*if (presentation_implementations.containsKey(presentation)) {
			        	error(element, "Presentation implemented multiple times (%s, %s).",
			        		  presentation_implementation, presentation_implementations.get(presentation));
			        	// Skips the current element
			        	continue;
			        }*/
			        
			        // Adds the Presentation implementation
			        presentation_implementations.put(presentation, (Class<? extends Activity>) presentation_implementation);
			        final String container = _element_utilities.getPackageOf(element).getQualifiedName().toString();
			        if (package_modules.containsKey(container))
		        		package_modules.get(container).add(element);
		        	else
		        		package_modules.put(container, newArrayList(element));     		
				}
			}
			return presentation_implementations;
		}
		
		/**
		 * <p>Produces an error message with the given information.</p>
		 * @param element The element to relate the error message to
		 * @param message The message to send
		 * @param arguments Arguments to format into the message
		 */
		private void error(Element element, String message, Object... arguments) {
			processingEnv.getMessager().printMessage(ERROR,
					String.format(message, arguments), element);
		}
		
		/**
		 * <p>Creates the {@link Configuration} for retrieving and processing source code templates.</p>
		 * @return The created configuration
		 */
		private Configuration createTemplateConfiguration() {
			// WORKAROUND: log4j is not found, so this disables logging to prevent the error
	        try {
				freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
			} catch (ClassNotFoundException exception) {
				processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
			}	        
	        final Configuration template_configuration = new Configuration();
	        template_configuration.setClassForTemplateLoading(getClass(), _TEMPLATE_DIRECTORY);
	        return template_configuration;
		}
		
		/**
		 * <p>Container for Presentation Controller binding data. Relates an @Controller to an @Presentation's 
		 * implementation and provides a name to use to refer to an instance of the @Controller's implementation.</p>
		 * @author Dandre Allison
		 */
		@SuppressWarnings("serial")
		private static class PresentationControllerBinding extends SimpleHash {
			
			/**
			 * <p>Constructs a {@link PresentationControllerBinding}.</p>
			 * @param controller The @Controller
			 * @param presentation_implementation The implementation of the @Controller's @Presentation 
			 */
			public PresentationControllerBinding(TypeElement controller, TypeElement presentation_implementation) {
				put(_CLASS_NAME, controller.getSimpleName().toString());
				// TODO: use correct naming convention
				put(_VARIABLE_NAME, controller.getSimpleName().toString());
				put(_PRESENTATION_IMPLEMENTATION, presentation_implementation.getSimpleName().toString());
			}
			
			private static final String _CLASS_NAME = "className";
			private static final String _VARIABLE_NAME = "variableName";
			private static final String _PRESENTATION_IMPLEMENTATION = "presentationImplementation";
		}
		
		/**
		 * <p>Indicates an error occurred while getting a {@link Class} from an {@link Element}.</p>
		 * @author Dandre Allison
		 */
		@SuppressWarnings("serial")
		private static class ClassFromElementException extends RuntimeException { 
			
			/**
			 * <p>Constructs a {@link ClassFromElementException}.</p>
			 * @param exception The thrown exception
			 */
			public ClassFromElementException(Exception exception) {
				super(exception);
			}
		}
		
		/** Location on the class path where the templates are found */
		private static final String _TEMPLATE_DIRECTORY = "/templates/";
		/** Name of the ModelViewControllerModule source code template */
		private static final String _MODEL_VIEW_CONTROLLER_MODULE_TEMPLATE = "ModelViewControllerModuleTemplate.ftl";
		/** Name of the SegueController source code template*/
		private static final String _SEGUE_CONTROLLER_TEMPLATE = "SegueControllerTemplate.ftl";
		/** Qualified name for the ModelViewControllerModule source code */
		private static final String _MODEL_VIEW_CONTROLLER_MODULE_SOURCE = "com.imminentmeals.prestige.ModelViewControllerModule";
		/** Qualified name for the SegueController source code */
		private static final String _SEGUE_CONTROLLER_SOURCE = "com.imminentmeals.prestige.SegueController";
		/** Counts the number of passes The Annotation Processor has performed */
		private int _passes = 0;
		private Elements _element_utilities;
		private Types _type_utilities;
		private final Reflections _reflections = new Reflections("");
	}
	
/* Private Constructor */
	/** <p>Prevents the {@link Prestige} from being constructed.</p> */
	private Prestige() { }
}
