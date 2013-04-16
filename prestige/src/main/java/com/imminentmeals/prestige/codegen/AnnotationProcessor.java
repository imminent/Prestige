package com.imminentmeals.prestige.codegen;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformEntries;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Syntax;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import android.app.Activity;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.io.Closeables;
import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Controller.Default;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.InjectDataSource;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.Presentation.NoProtocol;
import com.imminentmeals.prestige.annotations.PresentationImplementation;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;

@SupportedAnnotationTypes({ "com.imminentmeals.prestige.annotations.Presentation",
	                        "com.imminentmeals.prestige.annotations.PresentationImplementation",
	                        "com.imminentmeals.prestige.annotations.InjectDataSource",
                            "com.imminentmeals.prestige.annotations.Controller",
                            "com.imminentmeals.prestige.annotations.ControllerImplementation" })
public class AnnotationProcessor extends AbstractProcessor {	
	public static final String DATA_SOURCE_SUFFIX = "$$DataSourceInjector";
	public static final String CONTROLLER_MODULE_SUFFIX = "ControllerModule";
	
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
		final List<AnnotationProcessor.PresentationControllerBinding> presentation_controller_bindings = newArrayList();
		final Map<String, List<AnnotationProcessor.ControllerData>> controllers = newHashMap();
		final List<AnnotationProcessor.DataSourceInjectionData> data_source_injections = newArrayList();
		
		// Process the @Presentation annotations
		final ImmutableMap<Element, AnnotationProcessor.PresentationData> presentations = processPresentations(environment);
		
		// Process the @InjectDataSource annotations
		processDataSourceInjections(environment, data_source_injections, presentations);
		
		if (!presentations.isEmpty())
			System.out.println("Presentations data is " + Joiner.on(", ").join(presentations.entrySet()));
		
		// Process the @Controller annotations and @ControllerImplementation annotations per @Controller annotation
		processControllers(environment, presentation_controller_bindings, controllers, presentations);
		
		if (!presentation_controller_bindings.isEmpty())
			System.out.println("Presentation Controller bindings are " + Joiner.on(", ").join(presentation_controller_bindings));
		
		// Reformats the gathered information to be used in data models
		final ImmutableList.Builder<ModuleData> controller_modules = ImmutableList.<ModuleData>builder();
		final ImmutableList.Builder<Map<String, Object>> controller_module_data_models_builder = 
				ImmutableList.<Map<String, Object>>builder();
		for (Map.Entry<String, List<AnnotationProcessor.ControllerData>> controller_implementations : controllers.entrySet()) {
			final Element implementation = controller_implementations.getValue().get(0).implementation;
			final String package_name = _element_utilities.getPackageOf(implementation).getQualifiedName() + "";
			final String class_name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, 
					controller_implementations.getKey() + "ControllerModule");
			controller_module_data_models_builder.add(ImmutableMap.of(
					"className", class_name, 
					"qualifiedName", String.format("%s.%s", package_name, class_name),
					"package", package_name,
					"controllers", controller_implementations.getValue()));
			controller_modules.add(new ModuleData(String.format("%s.%s", package_name, class_name), 
					                              controller_implementations.getKey()));
		}
		
		// Creates the data models used to generate the code
		final Map<String, ?> segue_controller_data_model = ImmutableMap.of(
                "controllers", presentation_controller_bindings,
                "modules", controller_modules.build());
		
		
		final List<Map<String, Object>> controller_module_data_models = controller_module_data_models_builder.build();
		
		final List<Map<String, Object>> data_source_injection_data_models = ImmutableList.copyOf(
				Lists.transform(data_source_injections, 
						new Function<AnnotationProcessor.DataSourceInjectionData, Map<String, Object>>() {

							@Override
							@Nullable
							public Map<String, Object> apply(@Nonnull AnnotationProcessor.DataSourceInjectionData data) {
								final String package_name = _element_utilities.getPackageOf(data.target) + "";
								final String element_class = _element_utilities.getBinaryName((TypeElement) data.target) + "";
								return ImmutableMap.of(
										"package", package_name,
										"target", data.target,
										"variableName", data.variable.getSimpleName(),
										"className", element_class.substring(package_name.length() + 1) + DATA_SOURCE_SUFFIX);
							}
						}));
        
		// Generates the code
		generateSourceCode(template_configuration, segue_controller_data_model, controller_module_data_models, 
				           data_source_injection_data_models);
	     
		// Releases the annotation processing utilities
		_element_utilities = null;
		_type_utilities = null;
		
		return true;
	}

	/**
	 * <p>Processes the source code for the @Presentation and @PresentationImplementation annotations.</p>
	 * @param environment The round environment
	 */
	private ImmutableMap<Element, AnnotationProcessor.PresentationData> processPresentations(RoundEnvironment environment) {
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

			// Now that the @Presentation annotation has been verified and its data extracted find its implementations				
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
				presentation_implementations.put(element, implementation_element);
			}
		}
		
		System.out.println("Finished processing Presentations.");
		// TODO: Should require there to exist a @PresentationImplementation for @Controller to work?? Good?
		return ImmutableMap.copyOf(transformEntries(presentation_protocols, 
				new EntryTransformer<Element, Element, AnnotationProcessor.PresentationData>() {

					@Override
					public AnnotationProcessor.PresentationData transformEntry(@Nullable Element key, @Nullable Element protocol) {
						return new PresentationData(protocol, presentation_implementations.get(key));
					}
			
		}));
	}
	
	private void processDataSourceInjections(RoundEnvironment environment, 
			                                 List<AnnotationProcessor.DataSourceInjectionData> data_source_injections,
			                                 ImmutableMap<Element, AnnotationProcessor.PresentationData> presentations) {
		final TypeMirror no_protocol = _element_utilities.getTypeElement(NoProtocol.class.getCanonicalName()).asType();

		for (Element element : environment.getElementsAnnotatedWith(InjectDataSource.class)) {
			final TypeElement enclosing_element = (TypeElement) element.getEnclosingElement();
			System.out.println("@InjectDataSource is " + element);
			System.out.println("\tin " + enclosing_element);
			
			// Verifies containing type is a Presentation Implementation
	        if (enclosing_element.getAnnotation(PresentationImplementation.class) == null) {
	          error(element, "@InjectDataSource annotations must be specified in @PresentationImplementation classes (%s).",
	              enclosing_element);
	          // Skips the current element
	          continue;
	        }
	        
			
	        TypeMirror protocol = null;
	        for (AnnotationProcessor.PresentationData data : presentations.values())
	        	if (_type_utilities.isSameType(data.implementation.asType(), enclosing_element.asType())) {
	        		protocol = data.protocol.asType();
	        		break;
	        	}
	        System.out.println("\tdefined Protocol is " + protocol);
	        // Verifies that Presentation has a Protocol
	        if (_type_utilities.isSameType(protocol, no_protocol)) {
	        	error(element, "@InjectDataSource may only be used with Presentations that have a Protocol (%s).",
	        		  enclosing_element);
	        	// Skips the current element
	        	continue;
	        }
	        // Verifies that the target type is the Presentation's Protocol
	        if (!_type_utilities.isSameType(element.asType(), protocol)) {
	          error(element, "@InjectDataSource fields must by the same as the Presentation's Protocol (%s.%s).",
	              enclosing_element.getQualifiedName(), element);
	          // Skips the current element
	          continue;
	        }
	        	        
	        // Verify field properties.
	        Set<Modifier> modifiers = element.getModifiers();
	        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
	          error(element, "@InjectDataSource fields must not be private or static (%s.%s).",
	              enclosing_element.getQualifiedName(), element);
	          continue;
	        }
	        
	        // Gathers the @InjectDataSource information
	        data_source_injections.add(new DataSourceInjectionData(enclosing_element, element));
		}
	}
	
	/**
	 * <p>Processes the source code for the @Controller and @ControllerImplementation annotations.</p>
	 * @param environment The round environment
	 * @param presentation_controller_bindings Will hold the bindings between Presentation implementations and their 
	 *        Controllers 
	 * @param controllers Will hold the list of Controllers grouped under an implementation scope
	 * @param presentations The map of Presentations -> {@link PresentationData}
	 */
	private void processControllers(RoundEnvironment environment,
			                        List<AnnotationProcessor.PresentationControllerBinding> presentation_controller_bindings,
			                        Map<String, List<AnnotationProcessor.ControllerData>> controllers, 
			                        ImmutableMap<Element, AnnotationProcessor.PresentationData> presentations) {
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
				// TODO: Should require there to exist a @PresentationImplementation for @Controller to work?? Good?
				if (presentations.get(presentation).implementation != null)
					presentation_controller_bindings.add(new PresentationControllerBinding(element, 
							presentations.get(presentation).implementation));
			}
			
			// Now that the @Controller annotation has been verified and its data extracted find its implementations				
			// TODO: very inefficient
			for (Element implementation_element : environment.getElementsAnnotatedWith(ControllerImplementation.class)) {
				// Makes sure to only deal with Controller implementations for the current @Controller
				if (!_type_utilities.isSubtype(implementation_element.asType(), element.asType()))
					continue;
				
				System.out.println("\twith an implementation of " + implementation_element);
				
				// Gathers @ControllerImplementation information
				final String scope = implementation_element.getAnnotation(ControllerImplementation.class).value();
				final PackageElement package_name = _element_utilities.getPackageOf(implementation_element);
				final List<AnnotationProcessor.ControllerData> implementations = controllers.get(scope);
				if (implementations == null)
					controllers.put(scope, newArrayList(new ControllerData(element, implementation_element)));
				// Verifies that the scope-grouped @ControllerImplementations are in the same package
				else if (!_element_utilities.getPackageOf(implementations.get(0).implementation).equals(package_name)) {
					error(element, "All @ControllerImplementation(\"%s\") must be defined in the same package (%s).",
						  scope, implementation_element);
					// Skips the current element
					continue;
				} else
					implementations.add(new ControllerData(element, implementation_element));
			}
		}
		
		System.out.println("Finished processing Controllers.");
	}
	
	private void generateSourceCode(Configuration template_configuration, 
			                        Map<String, ?> segue_controller_data_model,
			                        List<Map<String, Object>> controller_module_data_models,
			                        List<Map<String,Object>> data_source_injection_data_models) {
		final Filer filer = processingEnv.getFiler();
		Writer writer = null;
		try {				
			// Generates the _SegueController
			JavaFileObject source_code = filer.createSourceFile(_SEGUE_CONTROLLER_SOURCE, (Element) null);
	        writer = source_code.openWriter();
	        writer.flush();
	        template_configuration.getTemplate(_SEGUE_CONTROLLER_TEMPLATE).process(segue_controller_data_model, writer);
	        writer.close();
	        
	        // Generates the *ControllerModules
	        for (Map<String, Object> controller_module_data_model : controller_module_data_models) {
	        	source_code = filer.createSourceFile((String) controller_module_data_model.get("qualifiedName"), 
	        			                             (Element) null);
	        	writer = source_code.openWriter();
	        	writer.flush();
	        	template_configuration.getTemplate(_CONTROLLER_MODULE_TEMPLATE).process(controller_module_data_model, writer);
	        	writer.close();
	        }
	        
	        // Generates the $$DataSourceInjectors
	        final Elements element_utilities = processingEnv.getElementUtils();
	        for (Map<String, Object> data_source_injection_data_model : data_source_injection_data_models) {
	        	final TypeElement element = (TypeElement) data_source_injection_data_model.get("target");
	        	source_code = filer.createSourceFile(element_utilities.getBinaryName(element) + DATA_SOURCE_SUFFIX, 
	        			                             element);
	        	writer = source_code.openWriter();
	        	template_configuration.getTemplate(_DATA_SOURCE_INJECTOR_TEMPLATE).process(data_source_injection_data_model, 
	        			                                                                   writer);
	        }
		} catch (IOException exception) {
			processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
		} catch (TemplateException exception) {
			processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
		} finally {
			try {
				Closeables.close(writer, writer != null);
			} catch (IOException exception) { }
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
			final String class_name = controller.getSimpleName() + "";
			put(_ELEMENT, controller);
			put(_VARIABLE_NAME, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, class_name));
			put(_PRESENTATION_IMPLEMENTATION, presentation_implementation);
		}
		
		private static final String _ELEMENT = "element";
		private static final String _VARIABLE_NAME = "variableName";
		private static final String _PRESENTATION_IMPLEMENTATION = "presentationImplementation";
	}
	
	/**
	 * <p>Container for @Controller data, groups the Controller interface with its implementation</p>.
	 * @author Dandre Allison
	 */
	@SuppressWarnings("serial")
	private static class ControllerData extends SimpleHash {
		/** The @ControllerImplementation stored for use in setting up code generation */
		public final Element implementation;
		
		/**
		 * <p>Constructs a {@link ControllerData}.<p>
		 * @param controller The @Controller 
		 * @param controller_implementation The implementation of the @Controller
		 */
		public ControllerData(Element controller, Element controller_implementation) {
			put(_INTERFACE, controller);
			put(_IMPLEMENTATION, controller_implementation.getSimpleName());
			implementation = controller_implementation;
		}
		
		private static final String _INTERFACE = "interface";
		private static final String _IMPLEMENTATION = "implementation";
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
		private static final String format = "{protocol: %s, implementation: %s}";
	}
	
	
	private static class DataSourceInjectionData {
		public final Element target;
		public final Element variable;
		
		/**
		 * <p>Constructs a {@link DataSourceInjectionData}.</p>
		 * @param target The target of the injection
		 * @param variable The variable from the target in which to inject the Data Source
		 */
		public DataSourceInjectionData(Element target, Element variable) {
			this.target = target;
			this.variable = variable;
		}
	}
	
	@SuppressWarnings("serial")
	private static class ModuleData extends SimpleHash {
		
		/**
		 * <p>Constructs a {@link ModuleData}.</p>
		 * @param element The module
		 * @param scope The implementation scope the module provides
		 */
		public ModuleData(String name, String scope) {
			put(_NAME, name);
			put(_SCOPE, scope);
		}
		
		private static final String _NAME = "name";
		private static final String _SCOPE = "scope";
	}
	
	/** Extracts the root from a Controller following the naming convention "*Controller" */
	private static final Matcher _CONTROLLER_TO_PRESENTATION = Pattern.compile("(.+)Controller").matcher("");
	/** Location on the class path where the templates are found */
	private static final String _TEMPLATE_DIRECTORY = "/templates/";
	/** Name of the ModelViewControllerModule source code template */
	private static final String _DATA_SOURCE_INJECTOR_TEMPLATE = "DataSourceInjectorTemplate.ftl";
	/** Name of the SegueController source code template */
	private static final String _SEGUE_CONTROLLER_TEMPLATE = "SegueControllerTemplate.ftl";
	/** Name of the *ControllerModule source code template */
	private static final String _CONTROLLER_MODULE_TEMPLATE = "ControllerModuleTemplate.ftl";
	/** Qualified name for the SegueController source code */
	private static final String _SEGUE_CONTROLLER_SOURCE = "com.imminentmeals.prestige._SegueController";
	/** Counts the number of passes The Annotation Processor has performed */
	private int _passes = 0;
	private Elements _element_utilities;
	private Types _type_utilities;
}