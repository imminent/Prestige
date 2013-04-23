package com.imminentmeals.prestige.codegen;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformEntries;
import static com.google.common.collect.Sets.newHashSet;
import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;
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

import javax.annotation.Nullable;
import javax.annotation.Syntax;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.io.Closeables;
import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.Prestige.Finder;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Controller.Default;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.InjectDataSource;
import com.imminentmeals.prestige.annotations.Model;
import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.Presentation.NoProtocol;
import com.imminentmeals.prestige.annotations.PresentationImplementation;
import com.imminentmeals.prestige.annotations.meta.Implementations;
import com.squareup.java.JavaWriter;

import dagger.Module;
import dagger.Provides;

// TODO: can you mandate that a default implementation is provided for everything that has any implementation?
@SupportedAnnotationTypes({ "com.imminentmeals.prestige.annotations.Presentation",
	                        "com.imminentmeals.prestige.annotations.PresentationImplementation",
	                        "com.imminentmeals.prestige.annotations.InjectDataSource",
                            "com.imminentmeals.prestige.annotations.Controller",
                            "com.imminentmeals.prestige.annotations.ControllerImplementation",
                            "com.imminentmeals.prestige.annotations.Model",
                            "com.imminentmeals.prestige.annotations.ModelImplementation" })
public class AnnotationProcessor extends AbstractProcessor {	
	public static final String DATA_SOURCE_INJECTOR_SUFFIX = "$$DataSourceInjector";
	public static final String CONTROLLER_MODULE_SUFFIX = "ControllerModule";
	public static final String MODEL_INJECTOR_SUFFIX = "$$ModelInjector";
	public static final String MODEL_MODULE_SUFFIX = "ModelModule";
	
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
		final List<PresentationControllerBinding> presentation_controller_bindings = newArrayList();
		final Map<String, List<ControllerData>> controllers = newHashMap();
		final List<DataSourceInjectionData> data_source_injections = newArrayList();
		final Map<String, List<ModelData>> models = newHashMap();
		
		// Process the @Presentation annotations
		final ImmutableMap<Element, PresentationData> presentations = processPresentations(environment);
		
		// Process the @InjectDataSource annotations
		processDataSourceInjections(environment, data_source_injections, presentations);
		
		if (!presentations.isEmpty())
			System.out.println("Presentations data is " + Joiner.on(", ").join(presentations.entrySet()));
		
		// Process the @Controller annotations and @ControllerImplementation annotations per @Controller annotation
		processControllers(environment, presentation_controller_bindings, controllers, presentations);
		
		// Process the @Model annotations
		processModels(environment, models);
		
		if (!presentation_controller_bindings.isEmpty())
			System.out.println("Presentation Controller bindings are " + Joiner.on(", ").join(presentation_controller_bindings));
		
		// Reformats the gathered information to be used in data models
		final ImmutableList.Builder<ModuleData> controller_modules = ImmutableList.<ModuleData>builder();
		for (Map.Entry<String, List<ControllerData>> controller_implementations : controllers.entrySet()) {
			final Element implementation = controller_implementations.getValue().get(0)._implementation;
			final String package_name = _element_utilities.getPackageOf(implementation).getQualifiedName() + "";
			final String class_name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, 
					controller_implementations.getKey()) + CONTROLLER_MODULE_SUFFIX;
			controller_modules.add(new ModuleData(String.format("%s.%s", package_name, class_name), 
					                              controller_implementations.getKey(),class_name, package_name,
					                              controller_implementations.getValue()));
		}
		final ImmutableList.Builder<ModuleData> model_modules = ImmutableList.<ModuleData>builder();
		for (Map.Entry<String, List<ModelData>> model_implementations : models.entrySet()) {
			final Element implementation = model_implementations.getValue().get(0)._implementation;
			final String package_name = _element_utilities.getPackageOf(implementation).getQualifiedName() + "";
			final String class_name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, 
					model_implementations.getKey()) + MODEL_MODULE_SUFFIX;
			model_modules.add(new ModuleData(String.format("%s.%s", package_name, class_name), 
					                              model_implementations.getKey(),class_name, package_name,
					                              model_implementations.getValue()));
		}
		
		// Generates the code
		generateSourceCode(presentation_controller_bindings, controller_modules.build(), data_source_injections,
				           model_modules.build());
	     
		// Releases the annotation processing utilities
		_element_utilities = null;
		_type_utilities = null;
		
		return true;
	}

	/**
	 * <p>Processes the source code for the @Presentation and @PresentationImplementation annotations.</p>
	 * @param environment The round environment
	 */
	private ImmutableMap<Element, PresentationData> processPresentations(RoundEnvironment environment) {
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
				new EntryTransformer<Element, Element, PresentationData>() {

					@Override
					public PresentationData transformEntry(@Nullable Element key, @Nullable Element protocol) {
						return new PresentationData(protocol, presentation_implementations.get(key));
					}
			
		}));
	}
	
	private void processDataSourceInjections(RoundEnvironment environment, 
			                                 List<DataSourceInjectionData> data_source_injections,
			                                 ImmutableMap<Element, PresentationData> presentations) {
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
			
	        TypeMirror protocol = no_protocol;
	        for (PresentationData data : presentations.values())
	        	if (data._implementation != null && 
	        	    _type_utilities.isSameType(data._implementation.asType(), enclosing_element.asType())) {
	        		protocol = data._protocol.asType();
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
	          error(element, "@InjectDataSource fields must be the same as the Presentation's Protocol (%s.%s).",
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
	        final String package_name = _element_utilities.getPackageOf(enclosing_element) + "";
			final String element_class = _element_utilities.getBinaryName((TypeElement) enclosing_element) + "";
	        data_source_injections.add(new DataSourceInjectionData(package_name, enclosing_element, element, element_class));
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
			                        List<PresentationControllerBinding> presentation_controller_bindings,
			                        Map<String, List<ControllerData>> controllers, 
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
						_CONTROLLER_TO_ROOT.reset(element.getSimpleName()+ "").replaceAll("$1Presentation");
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
			final Element protocol = presentations.get(presentation)._protocol;
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
				if (presentations.get(presentation)._implementation != null)
					presentation_controller_bindings.add(new PresentationControllerBinding(element, 
							presentations.get(presentation)._implementation));
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
				final List<ControllerData> implementations = controllers.get(scope);
				if (implementations == null)
					controllers.put(scope, newArrayList(new ControllerData(element, implementation_element)));
				// Verifies that the scope-grouped @ControllerImplementations are in the same package
				else if (!_element_utilities.getPackageOf(implementations.get(0)._implementation).equals(package_name)) {
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
	
	private void processModels(RoundEnvironment environment, Map<String, List<ModelData>> models) {
		for (Element element : environment.getElementsAnnotatedWith(Model.class)) {
			System.out.println("@Model is " + element);
			
			// Verifies that the target type is an interface
			if (element.getKind() != INTERFACE) {
				error(element, "@Model annotation may only be specified on interfaces (%s).",
					  element);
				// Skips the current element
				continue;
			} 
			
			// Verifies that the interface's visibility is public
			if (!element.getModifiers().contains(PUBLIC)) {
				error(element, "@Model interface must be public (%s).",
					  element);
				// Skips the current element
				continue;
			}
			
			// Now that the @Controller annotation has been verified and its data extracted find its implementations				
			// TODO: very inefficient
			for (Element implementation_element : environment.getElementsAnnotatedWith(ModelImplementation.class)) {
				// Makes sure to only deal with Model implementations for the current @Model
				if (!_type_utilities.isSubtype(implementation_element.asType(), element.asType()))
					continue;
				
				System.out.println("\twith an implementation of " + implementation_element);
				
				// Gathers @ModelImplementation information
				final String scope = implementation_element.getAnnotation(ModelImplementation.class).value();
				final PackageElement package_name = _element_utilities.getPackageOf(implementation_element);
				final List<ModelData> implementations = models.get(scope);
				if (implementations == null) {
					boolean should_generate_provider = true;
					for (Element member : ((TypeElement) implementation_element).getEnclosedElements()) {
						if (member.getAnnotation(Inject.class) == null) continue;
						else should_generate_provider = false;
						System.out.println("\t\tdoesn't need a generated @Provides");
					}
					models.put(scope, newArrayList(new ModelData(element, implementation_element, should_generate_provider)));
				// Verifies that the scope-grouped @ControllerImplementations are in the same package
				} else if (!_element_utilities.getPackageOf(implementations.get(0)._implementation).equals(package_name)) {
					error(element, "All @ModelImplementation(\"%s\") must be defined in the same package (%s).",
						  scope, implementation_element);
					// Skips the current element
					continue;
				} else {
					boolean should_generate_provider = true;
					for (Element member : ((TypeElement) implementation_element).getEnclosedElements()) {
						if (member.getAnnotation(Inject.class) == null) continue;
						else should_generate_provider = false;
						System.out.println("\t\tdoesn't need a generated @Provides");
					}
					implementations.add(new ModelData(element, implementation_element, should_generate_provider));
				}
			}
		}
		System.out.println("Finished processing Models.");
	}
	
	@SuppressWarnings("unchecked")
	private void generateSourceCode(List<PresentationControllerBinding> controllers, List<ModuleData> controller_modules,
			                        List<DataSourceInjectionData> data_source_injections,
			                        List<ModuleData> model_modules) {
		final Filer filer = processingEnv.getFiler();
		Writer writer = null;
		try {				
			// Generates the _SegueController
			JavaFileObject source_code = filer.createSourceFile(_SEGUE_CONTROLLER_SOURCE, (Element) null);
	        writer = source_code.openWriter();
	        writer.flush();
	        genereateSegueControllerSourceCode(writer, controllers, controller_modules, model_modules);
	        
	        // Generates the *ControllerModules
	        for (ModuleData controller_module : controller_modules) {
	        	source_code = filer.createSourceFile(controller_module._qualified_name, (Element) null);
	        	writer = source_code.openWriter();
	        	writer.flush();
	        	generateControllerModule(writer, controller_module._package_name, 
	        			                 (List<ControllerData>) controller_module._components, controller_module._class_name);
	        }
	        
	        // Generates the $$DataSourceInjectors
	        final Elements element_utilities = processingEnv.getElementUtils();
	        for (DataSourceInjectionData data_source_injection : data_source_injections) {
	        	final TypeElement element = (TypeElement) data_source_injection._target;
	        	source_code = filer.createSourceFile(element_utilities.getBinaryName(element) + DATA_SOURCE_INJECTOR_SUFFIX, 
	        			                             element);
	        	writer = source_code.openWriter();
	        	writer.flush();
	        	generateDataSourceInjector(writer, data_source_injection._package_name, data_source_injection._target,
	        			                   data_source_injection._variable_name, data_source_injection._class_name);
	        }
	        
	        // Generates the *ModelModules
	        for (ModuleData model_module : model_modules) {
	        	source_code = filer.createSourceFile(model_module._qualified_name, (Element) null);
	        	writer = source_code.openWriter();
	        	writer.flush();
	        	generateModelModule(writer, model_module._package_name, (List<ModelData>) model_module._components,
	        			            model_module._class_name);
	        }
	        
	        // Generates the $$ModelInjectors
		} catch (IOException exception) {
			processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
		} finally {
			try {
				Closeables.close(writer, writer != null);
			} catch (IOException exception) { }
		}
	}

	/**
	 * @param segue_controller_data_model
	 * @param writer
	 * @throws IOException
	 */
	private void genereateSegueControllerSourceCode(Writer writer, List<PresentationControllerBinding> controllers,
			                                        List<ModuleData> controller_modules, List<ModuleData> model_modules)
			                                        		throws IOException {
		JavaWriter java_writer = new JavaWriter(writer);
		java_writer.emitEndOfLineComment("Generated code from Prestige. Do not modify!")
				   .emitPackage("com.imminentmeals.prestige")
				   .emitStaticImports("com.google.common.collect.Lists.newArrayList",
						              "com.google.common.collect.Maps.newHashMap")
				   .emitImports("java.util.HashMap",
						        "java.util.List",
						        "java.util.Map",
						        "javax.inject.Inject",
						        "javax.inject.Named",
						        "javax.inject.Provider",
						        "android.app.Activity",
						        "com.google.common.collect.ImmutableMap",
						        "com.imminentmeals.prestige.ControllerContract",
						        "com.squareup.otto.Bus",
						        "dagger.Module",
						        "dagger.ObjectGraph")
					.emitEmptyLine()
					.emitJavadoc("<p>A Segue Controller that handles getting the appropriate Controller\n" +
							     "for the current Presentation, and communicating with the Controller Bus.</p>")
				    .beginType("com.imminentmeals.prestige._SegueController", "class", java.lang.reflect.Modifier.PUBLIC, null, 
				    		   // implements
				    		   "com.imminentmeals.prestige.SegueController")
				    .emitJavadoc("Bus over which Presentations communicate to their Controllers")
				    .emitAnnotation(Inject.class)
				    .emitAnnotation(Named.class, ControllerContract.BUS)
				    .emitField("com.squareup.otto.Bus", "controller_bus", 0);
		final StringBuilder puts = new StringBuilder();
		for (PresentationControllerBinding binding : controllers) {
			java_writer.emitJavadoc("Provider for instances of the {@link %s} Controller", binding._controller)
			           .emitAnnotation(Inject.class)
			           .emitField("javax.inject.Provider<" + binding._controller + ">", binding._variable_name, 0);
			puts.append(String.format(".put(%s.class, %s)\n", 
					    binding._presentation_implementation, binding._variable_name));
		}
		// Constructor
		java_writer.emitEmptyLine()
		           .emitJavadoc("<p>Constructs a {@link SegueController}.</p>")
		           .beginMethod(null, "com.imminentmeals.prestige._SegueController", java.lang.reflect.Modifier.PUBLIC, 
		        		        "java.lang.String", "scope")
		           .emitStatement("List<Object> modules = newArrayList()")
		           .emitEndOfLineComment("Controller modules");
		if (!controller_modules.isEmpty()) {
			for (ModuleData controller_module : controller_modules)
				if (controller_module._scope.equals(Implementations.PRODUCTION)) {
					java_writer.emitStatement("modules.add(new %s())", controller_module._qualified_name);
					break;
				}
			java_writer.beginControlFlow("if (scope.equals(\"" + controller_modules.get(0)._scope + "\"))")
		               .emitStatement("modules.add(new %s())", controller_modules.get(0)._qualified_name)
		               .endControlFlow();
			for (ModuleData module : controller_modules.subList(1, controller_modules.size()))
				java_writer.beginControlFlow("else if (scope.equals(\"" + module._scope + "\"))")
		                   .emitStatement("modules.add(new %s())", module._qualified_name)
		                   .endControlFlow();
		}
		java_writer.emitEndOfLineComment("Model modules");
		if (!model_modules.isEmpty()) {
			for (ModuleData model_module : model_modules)
				if (model_module._scope.equals(Implementations.PRODUCTION)) {
					java_writer.emitStatement("modules.add(new %s())", model_module._qualified_name);
					break;
				}
			java_writer.beginControlFlow("if (scope.equals(\"" + model_modules.get(0)._scope + "\"))")
		               .emitStatement("modules.add(new %s())", model_modules.get(0)._qualified_name)
		               .endControlFlow();
			for (ModuleData module : model_modules.subList(1, model_modules.size()))
				java_writer.beginControlFlow("else if (scope.equals(\"" + module._scope + "\"))")
		        .emitStatement("modules.add(new %s())", module._qualified_name)
		        .endControlFlow();
		}
		java_writer.emitStatement("_object_graph = ObjectGraph.create(modules.toArray())")
		           .emitStatement("_object_graph.inject(this)")
		           .emitStatement(
				"_presentation_controllers = ImmutableMap.<Class<?>, Provider<? extends ControllerContract>>builder()\n" +
				puts.toString() +
				".build()")
				   .emitStatement("_controllers = newHashMap()")
				   .endMethod()
				   .emitEmptyLine()
				   // SegueController Contract 
				   .emitAnnotation(SuppressWarnings.class, JavaWriter.stringLiteral("unchecked"))
				   .emitAnnotation(Override.class)
				   .beginMethod("<T> T", "dataSource", java.lang.reflect.Modifier.PUBLIC, "Class<?>", "target")
				   .emitStatement("return (T) _controllers.get(target)")
				   .endMethod()
				   .emitEmptyLine()
				   .emitAnnotation(Override.class)
				   .beginMethod("void", "sendMessage", java.lang.reflect.Modifier.PUBLIC, "Object", "message")
				   .emitStatement("controller_bus.post(message)")
				   .endMethod()
				   .emitEmptyLine()
				   .emitAnnotation(Override.class)
				   .beginMethod("void", "createController", java.lang.reflect.Modifier.PUBLIC, 
						        JavaWriter.type(Activity.class), "activity")
				   .emitStatement("final Class<?> activity_class = activity.getClass()")
				   .emitStatement("if (!_presentation_controllers.containsKey(activity_class)) return")
				   .emitEmptyLine()
				   .emitStatement("final ControllerContract controller = _presentation_controllers.get(activity_class).get()")
				   .emitStatement("controller.attachPresentation(activity)")
				   .emitStatement("controller_bus.register(controller)")
				   .emitStatement("_object_graph.inject(controller)")
				   .emitStatement("_controllers.put(activity_class, controller)")
				   .endMethod()
				   .emitEmptyLine()
				   .emitAnnotation(Override.class)
				   .beginMethod("void", "didDestroyActivity", java.lang.reflect.Modifier.PUBLIC, 
						   JavaWriter.type(Activity.class), "activity")
				   .emitStatement("final Class<?> activity_class = activity.getClass()")
				   .emitStatement("if (!_presentation_controllers.containsKey(activity_class)) return")
				   .emitEmptyLine()
				   .emitStatement("controller_bus.unregister(_controllers.get(activity_class))")
				   .emitStatement("_controllers.remove(activity_class)")
				   .endMethod()
				   .emitEmptyLine()
				   // Private fields
				   .emitJavadoc("Dependency injection object graph")
				   .emitField("ObjectGraph", "_object_graph", 
						      java.lang.reflect.Modifier.PRIVATE | java.lang.reflect.Modifier.FINAL)
				   .emitJavadoc("Provides the Controller implementation for the given Presentation Implementation")
				   .emitField("ImmutableMap<Class<?>, Provider<? extends ControllerContract>>",
						      "_presentation_controllers",
						      java.lang.reflect.Modifier.PRIVATE | java.lang.reflect.Modifier.FINAL)
				   .emitJavadoc("Maintains the Controller references as they are being used")
				   .emitField("Map<Class<?>, ControllerContract>",
						      "_controllers",
						      java.lang.reflect.Modifier.PRIVATE | java.lang.reflect.Modifier.FINAL)
				   .endType()
				   .emitEmptyLine();
		java_writer.close();
	}
	
	/**
	 * @param writer
	 * @param _package_name
	 * @param _components
	 * @param _class_name
	 */
	private void generateControllerModule(Writer writer, String package_name, List<ControllerData> controllers, 
			                             String class_name) throws IOException {
		JavaWriter java_writer = new JavaWriter(writer);
		java_writer.emitEndOfLineComment("Generated code from Prestige. Do not modify!")
				   .emitPackage(package_name)
				   .emitImports("javax.inject.Named",
						        "com.imminentmeals.prestige._SegueController",
						        "com.squareup.otto.Bus",
						        "dagger.Module",
						        "dagger.Provides",
						        "javax.inject.Singleton")
					.emitEmptyLine();
		final StringBuilder controller_list = new StringBuilder();
		for (ControllerData controller : controllers) {
			controller_list.append("<li>{@link " + controller._interface + "}</li>\n");
		}
		java_writer.emitJavadoc("<p>Module for injecting:\n" +
				                "<ul>\n" +
				                "%s" +
				                "</ul></p>", controller_list)
				   .emitAnnotation(Module.class, ImmutableMap.of(
						   "entryPoints", 
						   "{\n" +
							   Joiner.on(",\n").join(Lists.asList("_SegueController.class", Lists.transform(controllers, 
									   new Function<ControllerData, String>() {

										@Override
										@Nullable public String apply(@Nullable ControllerData controller) {
											return controller._implementation + ".class";
										}										
								}).toArray())) +
						   "\n}",
						   "overrides", !class_name.equals(_DEFAULT_CONTROLLER_MODULE),
						   "complete", false))
					.beginType(class_name, "class", java.lang.reflect.Modifier.PUBLIC)
					.emitEmptyLine()
					.emitAnnotation(Provides.class)
					.emitAnnotation(Singleton.class)
					.emitAnnotation(Named.class, ControllerContract.BUS)
					.beginMethod("com.squareup.otto.Bus", "providesControllerBus", 0)
					.emitStatement("return new Bus(\"Controller Bus\")")
					.endMethod();
		// Controller providers
		for (ControllerData controller : controllers)
			java_writer.emitEmptyLine()
			           .emitAnnotation(Provides.class)
			           .beginMethod(controller._interface + "", "provides" + controller._interface.getSimpleName(), 0)
			           .emitStatement("return new %s()", controller._implementation)
			           .endMethod();
		java_writer.endType();
		java_writer.close();
	}
	
	/**
	 * @param writer
	 * @param _package_name
	 * @param _target
	 * @param _variable_name
	 * @param _class_name
	 */
	private void generateDataSourceInjector(Writer writer, String package_name, Element target, String variable_name,
			                                String class_name) throws IOException {
		final JavaWriter java_writer = new JavaWriter(writer);
		java_writer.emitEndOfLineComment("Generated code from Prestige. Do not modify!")
		           .emitPackage(package_name)
		           .emitImports("com.imminentmeals.prestige.Prestige.Finder")
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Data Source into {@link %s}'s %s.</p>", target, variable_name)
			       .beginType(class_name, "class", java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.FINAL)
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Data Source into {@link %s}'s %s.</p>\n" +
			       		        "@param finder The finder that specifies how to retrieve the Segue Controller from the target\n" +
			       		        "@param target The target of the injection", target, variable_name)
			       .beginMethod("void", "injectDataSource", java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC, 
			    		        JavaWriter.type(Finder.class), "finder", 
			    		        processingEnv.getElementUtils().getBinaryName((TypeElement) target) + "", "target")
			       .emitStatement("target.%s = " +
			       		"finder.findSegueControllerApplication(target).segueController().dataSource(target.getClass())", 
			       		variable_name)
			       .endMethod()
			       .endType()
			       .emitEmptyLine();
		java_writer.close();
	}
	
	/**
	 * @param writer
	 * @param _package_name
	 * @param _components
	 * @param _class_name
	 */
	private void generateModelModule(Writer writer, String package_name, List<ModelData> models, String class_name) 
			throws IOException {
		JavaWriter java_writer = new JavaWriter(writer);
		java_writer.emitEndOfLineComment("Generated code from Prestige. Do not modify!")
				   .emitPackage(package_name)
				   .emitImports("javax.inject.Named",
						        "com.imminentmeals.prestige._SegueController",
						        "dagger.Module",
						        "dagger.Provides",
						        "javax.inject.Singleton")
					.emitEmptyLine();
		final StringBuilder model_list = new StringBuilder();
		for (ModelData model : models) {
			model_list.append("<li>{@link " + model._interface + "}</li>\n");
		}
		java_writer.emitJavadoc("<p>Module for injecting:\n" +
				                "<ul>\n" +
				                "%s" +
				                "</ul></p>", model_list)
				   .emitAnnotation(Module.class, ImmutableMap.of(
						   "entryPoints", 
						   "{\n" +
							   Joiner.on(",\n").join(Lists.asList("_SegueController.class", Lists.transform(models, 
									   new Function<ModelData, String>() {

										@Override
										@Nullable public String apply(@Nullable ModelData model) {
											return model._implementation + ".class";
										}										
								}).toArray())) +
						   "\n}",
						   "overrides", !class_name.equals(_DEFAULT_MODEL_MODULE),
						   "complete", false))
					.beginType(class_name, "class", java.lang.reflect.Modifier.PUBLIC)
					.emitEmptyLine();
		// Model providers
		for (ModelData model : models)
			if (model._should_generate_provider)
				java_writer.emitEmptyLine()
				           .emitAnnotation(Provides.class)
				           .beginMethod(model._interface + "", "provides" + model._interface.getSimpleName(), 0)
				           .emitStatement("return new %s()", model._implementation)
				           .endMethod();
			else {
				final String implementation = 
						CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, model._implementation.getSimpleName() + "");
				java_writer.emitEmptyLine()
				           .emitAnnotation(Provides.class)
				           .beginMethod(model._interface + "", "provides" + model._interface.getSimpleName(), 0, 
				        	            model._implementation + "", implementation)
				           .emitStatement("return %s", implementation)
				           .endMethod();
			}
		java_writer.endType();
		java_writer.close();
	}
	
	/**
	 * @param writer
	 * @param _package_name
	 * @param _target
	 * @param _variable_name
	 * @param _class_name
	 */
	/*private void generateModelInjector(Writer writer, String package_name, List<ModelInjectionData> injection,
			                                String class_name) throws IOException {
		final JavaWriter java_writer = new JavaWriter(writer);
		java_writer.emitEndOfLineComment("Generated code from Prestige. Do not modify!")
		           .emitPackage(package_name)
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Models into {@link %s}.</p>", class_name)
			       .beginType(class_name, "class", java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.FINAL)
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Data Source into {@link %s}'s %s.</p>\n" +
			       		        "@param finder The finder that specifies how to retrieve the Segue Controller from the target\n" +
			       		        "@param target The target of the injection", target, variable_name)
			       .beginMethod("void", "injectDataSource", java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC, 
			    		        JavaWriter.type(Finder.class), "finder", 
			    		        processingEnv.getElementUtils().getBinaryName((TypeElement) target) + "", "target")
			       .emitStatement("target.%s = " +
			       		"finder.findSegueControllerApplication(target).segueController().model(target.getClass())", 
			       		variable_name)
			       .endMethod()
			       .endType()
			       .emitEmptyLine();
		java_writer.close();
	}*/
	
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
	 * <p>Container for Presentation Controller binding data. Relates an @Controller to an @Presentation's 
	 * implementation and provides a name to use to refer to an instance of the @Controller's implementation.</p>
	 * @author Dandre Allison
	 */
	private static class PresentationControllerBinding {
		
		private final Element _controller;
		private final String _variable_name;
		private final Element _presentation_implementation;
		
		/**
		 * <p>Constructs a {@link PresentationControllerBinding}.</p>
		 * @param controller The @Controller
		 * @param presentation_implementation The implementation of the @Controller's @Presentation 
		 */
		public PresentationControllerBinding(Element controller, Element presentation_implementation) {
			final String class_name = controller.getSimpleName() + "";
			_controller = controller;
			_variable_name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, class_name);
			_presentation_implementation = presentation_implementation;
		}
	}
	
	/**
	 * <p>Container for @Controller data, groups the Controller interface with its implementation</p>.
	 * @author Dandre Allison
	 */
	private static class ControllerData {
		/** The @ControllerImplementation stored for use in setting up code generation */
		private final Element _implementation;
		private final Element _interface;
		
		/**
		 * <p>Constructs a {@link ControllerData}.<p>
		 * @param controller The @Controller 
		 * @param controller_implementation The implementation of the @Controller
		 */
		public ControllerData(Element controller, Element controller_implementation) {
			_implementation = controller_implementation;
			_interface = controller;
		}
	}
	
	/**
	 * <p>Container for @Model data, groups the Model interface with its implementation</p>.
	 * @author Dandre Allison
	 */
	private static class ModelData {
		/** The @ModelImplementation stored for use in setting up code generation */
		private final Element _implementation;
		private final Element _interface;
		private final boolean _should_generate_provider;
		
		/**
		 * <p>
		 * Constructs a {@link ModelData}.
		 * <p>
		 * 
		 * @param model The @Model
		 * @param model_implementation The implementation of the @Model
		 */
		public ModelData(Element model, Element model_implementation, boolean should_generate_provider) {
			_implementation = model_implementation;
			_interface = model;
			_should_generate_provider = should_generate_provider;
		}
	}
	
	/**
	 * <p>Container for Presentation data.</p>
	 * @author Dandre Allison
	 */
	private static class PresentationData {
		/** The Protocol */
		private final Element _protocol;
		/** The Presentation implementation */
		private final Element _implementation;
		
		/**
		 * <p>Constructs a {@link PresentationData}.</p>
		 * @param protocol The Protocol
		 * @param implementation The presentation implementation
		 */
		public PresentationData(Element protocol, Element implementation) {
			_protocol = protocol;
			_implementation = implementation;
		}

		@Override
		public String toString() {
			return String.format(format, _protocol, _implementation);
		}
		
		@Syntax("RegEx")
		private static final String format = "{protocol: %s, implementation: %s}";
	}
	
	private static class ModuleData {
		private final String _qualified_name;
		private final String _scope;
		private final String _class_name;
		private final String _package_name;
		private final List<?> _components;
		
		/**
		 * <p>Constructs a {@link ModuleData}.</p>
		 * @param element The module
		 * @param scope The implementation scope the module provides
		 */
		public ModuleData(String name, String scope, String class_name, String package_name, List<?> components) {
			_qualified_name = name;
			_scope = scope;
			_class_name = class_name;
			_package_name = package_name;
			_components = components;
		}
	}
	
	private static class DataSourceInjectionData {
		private final String _package_name;
		private final Element _target;
		private final String _variable_name;
		private final String _class_name;
		
		/**
		 * <p>Constructs a {@link DataSourceInjectionData}.</p>
		 * @param target The target of the injection
		 * @param variable The variable from the target in which to inject the Data Source
		 */
		public DataSourceInjectionData(String package_name, Element target, Element variable, String element_class) {
			_package_name = package_name;
			_target = target;
			_variable_name = variable.getSimpleName() + "";
			_class_name = element_class.substring(package_name.length() + 1) + DATA_SOURCE_INJECTOR_SUFFIX;
		}
	}
	
	private static class ModelInjectionData {
		private final String _package_name;
		private final Element _target;
		private final String _variable_name;
		private final String _class_name;
		
		/**
		 * <p>Constructs a {@link ModelInjectionData}.</p>
		 * @param target The target of the injection
		 * @param variable The variable from the target in which to inject the Model
		 */
		public ModelInjectionData(String package_name, Element target, Element variable, String element_class) {
			_package_name = package_name;
			_target = target;
			_variable_name = variable.getSimpleName() + "";
			_class_name = element_class.substring(package_name.length() + 1) + MODEL_INJECTOR_SUFFIX;
		}
	}
	
	/** Extracts the root from a Controller following the naming convention "*Controller" */
	private static final Matcher _CONTROLLER_TO_ROOT = Pattern.compile("(.+)Controller").matcher("");
	/** Qualified name for the SegueController source code */
	private static final String _SEGUE_CONTROLLER_SOURCE = "com.imminentmeals.prestige._SegueController";
	private static final String _DEFAULT_CONTROLLER_MODULE = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, PRODUCTION) + 
			CONTROLLER_MODULE_SUFFIX;
	private static final String _DEFAULT_MODEL_MODULE = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, PRODUCTION) +
			MODEL_MODULE_SUFFIX;
	/** Counts the number of passes The Annotation Processor has performed */
	private int _passes = 0;
	private Elements _element_utilities;
	private Types _type_utilities;
}