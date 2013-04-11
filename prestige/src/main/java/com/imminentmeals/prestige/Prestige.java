package com.imminentmeals.prestige;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformEntries;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.Syntax;
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

import android.app.Activity;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.io.Closeables;
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
		                        "com.imminentmeals.prestige.annotations.PresentationImplementation",
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
			
			// Configures the templates generating the code        
	        final Configuration template_configuration = createTemplateConfiguration();
			
			// Initializes the data model subcomponents that will be used when generating the code
			final List<TypeElement> imports = newArrayList();
			final List<PresentationControllerBinding> presentation_controller_bindings = newArrayList();
			
			// Process the @Presentation annotations
			final ImmutableMap<Element, PresentationData> presentations = processPresentations(environment, imports);
			
			System.out.println("Presentation imports are " + Joiner.on(", ").join(imports));
			System.out.println("Presentations data is " + Joiner.on(", ").join(presentations.entrySet()));
			
			// Process the @Controller annotations and @ControllerImplementation annotations per @Controller annotation
			processControllers(environment, imports, presentation_controller_bindings, presentations);
			
			System.out.println("Presentation imports are " + Joiner.on(", ").join(imports));
			System.out.println("Presentation Controller bindings are " + Joiner.on(", ").join(presentation_controller_bindings));
						
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
					return new PresentationControllerBinding(controller, controller);
				}
			});*/
	        
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
				try {
					Closeables.close(writer, writer != null);
				} catch (IOException exception) { }
			}
		     
			// Releases the annotation processing utilities
			_element_utilities = null;
			_type_utilities = null;
			
			return true;
		}

		/**
		 * <p>Processes the source code for the @Presentation and @PresentationImplementation annotations.</p>
		 * @param environment The round environment
		 * @param imports Will hold the list of Presentation implementations
		 */
		private ImmutableMap<Element, PresentationData> processPresentations(RoundEnvironment environment,
				                                                        List<TypeElement> imports) {
			final TypeMirror activity_type = _element_utilities.getTypeElement(Activity.class.getCanonicalName()).asType();
			final Map<Element, Element> presentation_protocols = newHashMap();
			final Map<Element, Element> presentation_implementations = newHashMap();
						
			for (Element element : environment.getElementsAnnotatedWith(Presentation.class)) {
				System.out.println("@Presentation is " + element);
				
				// Verifies that the target type is an interface
				if (element.getKind() != INTERFACE) {
					error(element, "@Presentation annotation may only be specified on interfaces (%s).",
						  element);
					// Skips the current element
					continue;
				}
				
				// Verifies that the interface's visibility is public
				if (!element.getModifiers().contains(PUBLIC)) {
					error(element, "@Presentation interface must be public (%s).",
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
				
				// Verifies that the Protocol visibility is public
				if (!protocol.getModifiers().contains(PUBLIC)) {
					error(element, "@Presentation Protocol must be public (%s).",
							protocol);
					// Skips the current element
					continue;
				}
				
				// Adds the mapping of the Presentation to its Protocol (null if no Protocol is defined)
				presentation_protocols.put(element, protocol);

				// Now that the @Presentation annotation has been verified and its data extracted find the its implementations				
				// TODO: very inefficient
				for (Element implementation_element : environment.getElementsAnnotatedWith(PresentationImplementation.class)) {
					// Makes sure to only deal with Presentation implementations for the current @Presentation
					if (!_type_utilities.isSubtype(implementation_element.asType(), element.asType()))
						continue;
					
					System.out.println("\twith an implementation of " + implementation_element);
					
					// Verifies that the Presentation implementation extends from Activity
			        if (!_type_utilities.isSubtype(implementation_element.asType(), activity_type)) {
			          error(element, "@PresentationImplementation classes must extend from Activity (%s).",
			                element); 
			          // Skips the current element
			          continue;
			        }
			        
			        // Adds the implementation to the list of imports
					imports.add((TypeElement) implementation_element);
					presentation_implementations.put(element, implementation_element);
				}
			}
			
			System.out.println("Finished processing Presentations.");
			return ImmutableMap.copyOf(transformEntries(presentation_implementations, 
					new EntryTransformer<Element, Element, PresentationData>() {

						@Override
						public PresentationData transformEntry(@Nullable Element key, @Nullable Element implementation) {
							return new PresentationData(presentation_protocols.get(key), implementation);
						}
				
			}));
		}
		
		/**
		 * <p>Processes the source code for the @Controller and @ControllerImplementation annotations.</p>
		 * @param environment The round environment
		 * @param imports Will hold the list of Controllers
		 * @param presentation_controller_bindings Will hold the bindings between Presentation implementations and their 
		 *        Controllers 
		 * @param presentations The map of Presentations -> {@link PresentationData}
		 */
		private void processControllers(RoundEnvironment environment, List<TypeElement> imports,
				                        List<PresentationControllerBinding> presentation_controller_bindings,
				                        ImmutableMap<Element, PresentationData> presentations) {
			final TypeMirror controller_contract = 
					_element_utilities.getTypeElement(ControllerContract.class.getCanonicalName()).asType();
			final TypeMirror default_presentation = _element_utilities.getTypeElement(Default.class.getCanonicalName()).asType();
			final TypeMirror no_protocol = _element_utilities.getTypeElement(NoProtocol.class.getCanonicalName()).asType();
			
			for (Element element : environment.getElementsAnnotatedWith(Controller.class)) {
				System.out.println("@Controller is " + element);
				
				// Verifies that the target type is an interface
				if (element.getKind() != INTERFACE) {
					error(element, "@Controller annotation may only be specified on interfaces (%s).",
						  element);
					// Skips the current element
					continue;
				} 
				
				// Verifies that the interface's visibility is public
				if (!element.getModifiers().contains(PUBLIC)) {
					error(element, "@Controller interface must be public (%s).",
						  element);
					// Skips the current element
					continue;
				}
				
				// Verifies that the interface extends the ControllerContract interface
				if (!_type_utilities.isSubtype(element.asType(), controller_contract)) {
					error(element, "@Controller interface must extend the ControllerContract (%s).",
			              element); 
			          // Skips the current element
			          continue;
				}
				
				// Gathers @Controller information
				final Controller controller_annotation = element.getAnnotation(Controller.class);
				Element presentation = null;
				try {
					controller_annotation.presentation();
				} catch (MirroredTypeException exception) {
					presentation = _type_utilities.asElement(exception.getTypeMirror());
				}
				
				// Searches for a matching @Presentation if the Presentation is defined by naming convention
				if (_type_utilities.isSameType(presentation.asType(), default_presentation)) {
					final String presentation_from_controller_name = 
							_CONTROLLER_TO_PRESENTATION.reset(element.getSimpleName()+ "").replaceAll("$1Presentation");
					for (Element presentation_interface : presentations.keySet()) 
						if (presentation_interface.getSimpleName().contentEquals(presentation_from_controller_name)) {
							presentation = presentation_interface;
							break;
						}
				}
				
				System.out.println("\tfor Presentation: " + presentation);
				
				// Verifies that the Controller's Presentation is a Presentation
				if (!presentations.containsKey(presentation)) {
					error(element, "@Controller Presentation must be an @Presentation (%s).", element);
					// Skips the current element
					continue;
				}
				
				// Verifies that the Controller implements the Presentation's Protocol, if one is required
				final Element protocol = presentations.get(presentation).protocol;
				if (!(_type_utilities.isSubtype(protocol.asType(), no_protocol) || 
					  _type_utilities.isSubtype(element.asType(), protocol.asType()))) {
					error(element, "@Controller is required to implement Protocol %s by its Presentation (%s).",
						  protocol, element);
					// Skips the current element
					continue;
				}
				
				// Adds Presentation Controller binding if Controller has a Presentation
				if (!_type_utilities.isSameType(presentation.asType(), default_presentation)) {
					imports.add((TypeElement) element);
					presentation_controller_bindings.add(new PresentationControllerBinding(element, 
							presentations.get(presentation).implementation));
				}
			}
			
			System.out.println("Finished processing Controllers.");
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
		@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
		private void processControllers(RoundEnvironment environment, Map<TypeElement, Class<?>> presentation_protocols,
				Map<TypeElement, Class<? extends Activity>> presentation_implementations,
				Map<TypeElement, Map<String, Class>> presentation_controller_implementations,
				Map<Class<?>, String> controller_package_modules) {
			
			for (Element element : environment.getElementsAnnotatedWith(Controller.class)) {
				
				// Processes Controller implementations
				final Map<String, Class> controller_implementations = newHashMap();
				for (Class<?> controller_implementation : newArrayList(Controller.class)) {
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
				
				// Adds the Controller implementations to the list of all Controller implementations
				presentation_controller_implementations.put((TypeElement) element, controller_implementations);
			}
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
			System.out.println("Configuring template loading...");
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
			public PresentationControllerBinding(Element controller, Element presentation_implementation) {
				final String class_name = controller.getSimpleName().toString();
				put(_CLASS_NAME, class_name);
				put(_VARIABLE_NAME, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, class_name));
				put(_PRESENTATION_IMPLEMENTATION, presentation_implementation.getSimpleName().toString());
			}
			
			private static final String _CLASS_NAME = "className";
			private static final String _VARIABLE_NAME = "variableName";
			private static final String _PRESENTATION_IMPLEMENTATION = "presentationImplementation";
		}
		
		/**
		 * <p>Container for Presentation data.</p>
		 * @author Dandre Allison
		 */
		private static class PresentationData {
			/** The Protocol */
			public final Element protocol;
			/** The Presentation implementation */
			public final Element implementation;
			
			/**
			 * <p>Constructs a {@link PresentationData}.</p>
			 * @param protocol The Protocol
			 * @param implementation The presentation implementation
			 */
			public PresentationData(Element protocol, Element implementation) {
				this.protocol = protocol;
				this.implementation = implementation;
			}

			@Override
			public String toString() {
				return String.format(format, protocol, implementation);
			}
			
			@Syntax("RegEx")
			private static final String format = "protocol:\n\t%s\nimplementation:\n\t%s";
		}
		
		/** Extracts the root from a Controller following the naming convention "*Controller" */
		private static final Matcher _CONTROLLER_TO_PRESENTATION = Pattern.compile("(.+)Controller").matcher("");
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
	}
	
/* Private Constructor */
	/** <p>Prevents the {@link Prestige} from being constructed.</p> */
	private Prestige() { }
}
