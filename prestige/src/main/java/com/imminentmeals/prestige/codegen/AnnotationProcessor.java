package com.imminentmeals.prestige.codegen;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.imminentmeals.prestige.GsonProvider;
import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.annotations.Controller.Default;
import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.InjectDataSource;
import com.imminentmeals.prestige.annotations.InjectModel;
import com.imminentmeals.prestige.annotations.InjectPresentation;
import com.imminentmeals.prestige.annotations.InjectPresentationFragment;
import com.imminentmeals.prestige.annotations.Model;
import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.annotations.Presentation.NoProtocol;
import com.imminentmeals.prestige.annotations.PresentationFragment;
import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;
import com.imminentmeals.prestige.annotations.PresentationImplementation;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Maps.transformEntries;
import static com.google.common.collect.Sets.newHashSet;
import static com.imminentmeals.prestige.codegen.Utilities.getAnnotation;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;

// TODO: can you mandate that a default implementation is provided for everything that has any implementation?
// TODO: public static Strings for generated methods and classes to use in Prestige helper methods
@SuppressWarnings("UnnecessaryContinue")
@SupportedAnnotationTypes({ "com.imminentmeals.prestige.annotations.Presentation",
	                        "com.imminentmeals.prestige.annotations.PresentationImplementation",
	                        "com.imminentmeals.prestige.annotations.InjectDataSource",
                            "com.imminentmeals.prestige.annotations.Controller",
                            "com.imminentmeals.prestige.annotations.ControllerImplementation",
                            "com.imminentmeals.prestige.annotations.Model",
                            "com.imminentmeals.prestige.annotations.ModelImplementation",
                            "com.imminentmeals.prestige.annotations.PresentationFragment",
                            "com.imminentmeals.prestige.annotations.PresentationFragmentImplementation",
                            "com.imminentmeals.prestige.annotations.InjectPresentationFragment",
                            "com.imminentmeals.prestige.annotations.InjectPresentation" })
public class AnnotationProcessor extends AbstractProcessor {	
	public static final String DATA_SOURCE_INJECTOR_SUFFIX = "$$DataSourceInjector";
	public static final String CONTROLLER_MODULE_SUFFIX = "ControllerModule";
	public static final String MODEL_INJECTOR_SUFFIX = "$$ModelInjector";
	public static final String MODEL_MODULE_SUFFIX = "ModelModule";
	public static final String PRESENTATION_FRAGMENT_INJECTOR_SUFFIX = "$$PresentationFragmentInjector";
    public static final String PRESENTATION_INJECTOR_SUFFIX = "$$PresentationInjector";

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
		if (_passes++ > 0) return true;
		
		// Grabs the annotation processing utilities
		_element_utilities = processingEnv.getElementUtils();
		_type_utilities = processingEnv.getTypeUtils();
		
		// Initializes the data model subcomponents that will be used when generating the code
		final List<PresentationControllerBinding> presentation_controller_bindings = newArrayList();
		final Map<String, List<ControllerData>> controllers = newHashMap();
		final List<DataSourceInjectionData> data_source_injections = newArrayList();
		final Map<String, List<ModelData>> models = newHashMap();
		final List<ModelData> model_interfaces = newArrayList();
		final Map<Element, List<ModelInjectionData>> model_injections = newHashMap();
		final Map<Element, List<PresentationFragmentInjectionData>> presentation_fragment_injections = newHashMap();
		final Map<Element, List<PresentationFragmentInjectionData>> controller_presentation_fragment_injections = newHashMap();
        final Map<Element, PresentationInjectionData> controller_presentation_injections = newHashMap();
		
		// Processes the @PresentationFragment and @PresentationFragmentImplementation annotations
		final ImmutableMap<Element, PresentationFragmentData> presentation_fragments = processPresentationFragments(environment);
		
		// Processes the @Presentation and @PresentationImplementation annotations
		final ImmutableMap<Element, PresentationData> presentations = processPresentations(environment, presentation_fragments);
		
		// Processes the @InjectDataSource annotations
		processDataSourceInjections(environment, data_source_injections, presentations, presentation_fragments);
		
		// Processes the @InjectPresentationFragment annotations
		processPresentationFragmentInjections(environment, presentation_fragment_injections, 
				                              controller_presentation_fragment_injections, presentation_fragments);

    // Processes the @InjectPresentation annotations
    processPresentationInjections(environment, controller_presentation_injections);
		
		// Processes the @Controller annotations and @ControllerImplementation annotations per @Controller annotation
		processControllers(environment, presentation_controller_bindings, controllers, presentations);
		
		// Processes the @Model annotations
		processModels(environment, models, model_interfaces);
		
		// Processes the @InjectModel annotations
		processModelInjections(environment, model_injections, model_interfaces);
		
		// Reformats the gathered information to be used in data models
		final ImmutableList.Builder<ModuleData> controller_modules = ImmutableList.builder();
		for (Map.Entry<String, List<ControllerData>> controller_implementations : controllers.entrySet()) {
			final Element implementation = controller_implementations.getValue().get(0).implementation;
			final String package_name = _element_utilities.getPackageOf(implementation).getQualifiedName() + "";
			final String class_name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, 
					controller_implementations.getKey()) + CONTROLLER_MODULE_SUFFIX;
			controller_modules.add(new ModuleData(String.format("%s.%s", package_name, class_name), 
					                              controller_implementations.getKey(),class_name, package_name,
					                              controller_implementations.getValue()));
		}
		final ImmutableList.Builder<ModuleData> model_modules = ImmutableList.builder();
		for (Map.Entry<String, List<ModelData>> model_implementations : models.entrySet()) {
			final Element implementation = model_implementations.getValue().get(0).implementation;
			final String package_name = _element_utilities.getPackageOf(implementation).getQualifiedName() + "";
			final String class_name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, 
					model_implementations.getKey()) + MODEL_MODULE_SUFFIX;
			model_modules.add(new ModuleData(String.format("%s.%s", package_name, class_name), 
					                              model_implementations.getKey(),class_name, package_name,
					                              model_implementations.getValue()));
		}
		final Map<Element, Map<Integer, List<PresentationFragmentInjectionData>>> presentation_fragment_display_injections =
			Maps.transformValues(presentation_fragment_injections, 
				new Function<List<PresentationFragmentInjectionData>, Map<Integer, List<PresentationFragmentInjectionData>>>() {

					@Nullable public Map<Integer, List<PresentationFragmentInjectionData>> apply(
							@Nullable List<PresentationFragmentInjectionData> presentation_fragments) {
						final Map<Integer, List<PresentationFragmentInjectionData>> injections = newHashMap();
                        if (presentation_fragments == null) return ImmutableMap.copyOf(injections);
						for (PresentationFragmentInjectionData presentation_fragment : presentation_fragments) {
							if (presentation_fragment.is_manually_created)
								// Skips the current injection, since it will be handled manually
								continue;
							for (Entry<Integer, Integer> injection : presentation_fragment.displays.entrySet())
								if (injections.containsKey(injection.getKey()))
									injections.get(injection.getKey()).add(presentation_fragment);
								else
									injections.put(injection.getKey(), newArrayList(presentation_fragment));
						}
						return ImmutableMap.copyOf(injections);
					}
				});
        final ImmutableMap.Builder<Element, ModelData> element_to_model_map = ImmutableMap.builder();
        for (ModelData model : model_interfaces) element_to_model_map.put(model.contract, model);
        final Map<String, Map<Element, ModelData>> model_implementations = newHashMap();
        final int number_of_scopes = models.size();
        for (Entry<String, List<ModelData>> entry : models.entrySet()) {
            Map<Element, ModelData> scoped_models = model_implementations.get(entry.getKey());
            if (scoped_models == null) {
                scoped_models = newHashMapWithExpectedSize(number_of_scopes);
                model_implementations.put(entry.getKey(), scoped_models);
            }
            for (ModelData implementation : entry.getValue())
                scoped_models.put(implementation.contract, implementation);
        }
		
		// Generates the code
    final CodeGenerator generator = new CodeGenerator(processingEnv.getFiler()
        , processingEnv.getMessager(), _element_utilities, _type_utilities);
		generator.generateSourceCode(presentation_controller_bindings, controller_modules.build(),
        data_source_injections,
        model_modules.build(), model_interfaces, element_to_model_map.build(),
        ImmutableMap.copyOf(model_implementations), model_injections,
        ImmutableMap.copyOf(presentation_fragment_display_injections),
        ImmutableMap.copyOf(controller_presentation_fragment_injections),
        controller_presentation_injections);
	     
		// Releases the annotation processing utilities
		_element_utilities = null;
		_type_utilities = null;
		
		return true;
	}
	
	/**
	 * <p>Processes the source code for the @PresentationFragment and @PresentationFragmentImplementation annotations.</p>
	 * @param environment The round environment
	 * @return
	 */
	private ImmutableMap<Element, PresentationFragmentData> processPresentationFragments(RoundEnvironment environment) {
		final Map<Element, Element> presentation_fragment_protocols = newHashMap();
		final Map<Element, Element> presentation_fragment_implementations = newHashMap();
		final Map<Element, Set<Element>> unverified_presentation_fragments = newHashMap();
		final TypeMirror no_protocol = _element_utilities.getTypeElement(PresentationFragment.NoProtocol.class.getCanonicalName()).asType();

		for (Element element : environment.getElementsAnnotatedWith(PresentationFragment.class)) {
			note("@PresentationFragment is " + element);

            // Skips the current element if it isn't a public interface
            if (verifyPublicInterface(element
                                    , "@PresentationFragment annotation may only be specified on interfaces (%s)."
                                    , "@PresentationFragment interfaces must be public (%s).")) continue;


			// Gathers @PresentationFragment information
            final Map<String, Object> parsed_annotation = getAnnotation(PresentationFragment.class, element);

            if (parsed_annotation.get("protocol") == null) {
                error(element, "protocol for presentation %s is null.", element);
                // Skips the current element
                continue;
            }

            final Element protocol = getElement(parsed_annotation.get("protocol"));

            note("\twith Protocol: " + protocol);
            // Skips current element if protocol isn't a public interface
            if (verifyPublicInterface(protocol
                                    , "@PresentationFragment Protocol must be an interface (%s)."
                                    , "@PresentationFragment Protocol must be public (%s).")) continue;


            // Verifies previously unverified Presentation Fragments that use this Presentation Fragment
			// Notice that these were deferred until this Presentation Fragment was processed
			if (!_type_utilities.isSameType(protocol.asType(), no_protocol) && unverified_presentation_fragments.containsKey(element))
				for (Element presentation_fragment : unverified_presentation_fragments.get(element)) {
					final TypeMirror super_protocol = presentation_fragment_protocols.get(presentation_fragment).asType();
					if (!_type_utilities.isSubtype(super_protocol, protocol.asType())) {
						error(presentation_fragment, 
							  "@PresentationFragment Protocol must extend %s from Presentation Fragment %s (%s).",
							  protocol, element, presentation_fragment);
						// Skips the current element
						continue;
					} else {
						unverified_presentation_fragments.get(element).remove(presentation_fragment);
						if (unverified_presentation_fragments.get(element).isEmpty())
							unverified_presentation_fragments.remove(element);
					}
				}
			
			// Adds the mapping of the Presentation Fragment to its Protocol
			presentation_fragment_protocols.put(element, protocol);
		}

        // Now that the @PresentationFragment annotation has been verified and its data extracted find its implementations
        processPresentationFragmentImplementations(environment, presentation_fragment_protocols
                                                 , presentation_fragment_implementations, unverified_presentation_fragments
                                                 , no_protocol);

        // Verifies that all PresentationFragments have been verified
        verifyPresentationFragments(presentation_fragment_protocols, unverified_presentation_fragments);
		
		return ImmutableMap.copyOf(transformEntries(presentation_fragment_protocols,
				new EntryTransformer<Element, Element, PresentationFragmentData>() {

					public PresentationFragmentData transformEntry(@Nullable Element key, @Nullable Element protocol) { return new PresentationFragmentData(protocol == null? _type_utilities.asElement(no_protocol) : protocol
                                                                                          , presentation_fragment_implementations.get(key)); }
			
		}));
	}

    private boolean verifyPublicInterface(Element element, String not_interface, String not_public) {
        // Verifies that the Protocol is an Interface
        if (element.getKind() != INTERFACE) {
            error(element, not_interface, element);
            // Skips the current element
            return true;
        }

        // Verifies that the Protocol visibility is public
        if (!element.getModifiers().contains(PUBLIC)) {
            error(element, not_public, element);
            // Skips the current element
            return true;
        }
        return false;
    }

    private void verifyPresentationFragments(Map<Element, Element> presentation_fragment_protocols, Map<Element, Set<Element>> unverified_presentation_fragments) {
        final String format = "@PresentationFragment Protocol must extend %s from Presentation Fragment %s (%s).";
        for (Entry<Element, Set<Element>> entry : unverified_presentation_fragments.entrySet()) {
            final Element protocol = presentation_fragment_protocols.get(entry.getKey());
            if (protocol != null)
                for (Element element : entry.getValue())
                    error(element, format, protocol, entry.getKey(), element);
        }
    }

    /**
     * <p>Processes the source code for the @PresentationFragmentImplementation annotations.</p>
     * @param environment The round environment
     * @param presentation_fragment_protocols
     * @param presentation_fragment_implementations
     * @param unverified_presentation_fragments
     * @param no_protocol
     */
    private void processPresentationFragmentImplementations(RoundEnvironment environment
                                                          , Map<Element, Element> presentation_fragment_protocols
                                                          , Map<Element, Element> presentation_fragment_implementations
                                                          , Map<Element, Set<Element>> unverified_presentation_fragments
                                                          , TypeMirror no_protocol) {
        final Set<Element> presentation_fragments = newHashSet(presentation_fragment_protocols.keySet());
        for (Element implementation_element : environment.getElementsAnnotatedWith(PresentationFragmentImplementation.class)) {
            // Makes sure to only deal with Presentation Fragment implementations for the current @PresentationFragment
            Element presentation_fragment = null;
            for (Element element : presentation_fragments)
                if (_type_utilities.isSubtype(implementation_element.asType(), element.asType())) {
                    presentation_fragment = element;
                    presentation_fragments.remove(presentation_fragment);
                    break;
                }
            // Skips the current element
            if (presentation_fragment == null) continue;

            note(_WITH_AN_IMPLEMENTATION_OF + implementation_element);

            // Finds Presentation Fragment injections and verifies that their Protocols are met
            verifyPresentationFragmentSubProtocols(presentation_fragment_protocols
                                                 , unverified_presentation_fragments, no_protocol
                                                 , implementation_element, presentation_fragment);

            // Adds the implementation to the list of imports
            presentation_fragment_implementations.put(presentation_fragment, implementation_element);
        }
    }

    private void verifyPresentationFragmentSubProtocols(Map<Element, Element> presentation_fragment_protocols
                                                      , Map<Element, Set<Element>> unverified_presentation_fragments
                                                      , TypeMirror no_protocol, Element implementation_element
                                                      , Element presentation_fragment) {
        final TypeMirror protocol = presentation_fragment_protocols.get(presentation_fragment).asType();
        for (Element enclosed_element : implementation_element.getEnclosedElements()) {
            if (enclosed_element.getAnnotation(InjectPresentationFragment.class) != null &&
                    enclosed_element.getKind() == FIELD) {
                // Verifies that the Protocol extends all of the Presentation Fragment's Protocols
                // Notice that it only checks against the Presentation Fragments that have already been processed
                final Element sub_presentation_fragment = _type_utilities.asElement(enclosed_element.asType());
                note("\tcontains Presentation Fragment: " + sub_presentation_fragment);
                if (!_type_utilities.isSameType(presentation_fragment_protocols.get(sub_presentation_fragment).asType(), no_protocol)) {
                    final TypeMirror sub_protocol =
                            presentation_fragment_protocols.get(sub_presentation_fragment).asType();
                    if (!_type_utilities.isSubtype(protocol, sub_protocol)) {
                        error(implementation_element,
                                "@PresentationFragment Protocol must extend %s from Presentation Fragment %s (%s).",
                                sub_protocol, sub_presentation_fragment, implementation_element);
                        // Skips the current element
                        continue;
                    }
                } else if (unverified_presentation_fragments.containsKey(enclosed_element))
                    unverified_presentation_fragments.get(enclosed_element).add(presentation_fragment);
                else
                    unverified_presentation_fragments.put(enclosed_element, newHashSet(presentation_fragment));
            }
        }
    }

    /**
	 * <p>Processes the source code for the @Presentation and @PresentationImplementation annotations.</p>
	 * @param environment The round environment
	 */
	private ImmutableMap<Element, PresentationData> processPresentations(RoundEnvironment environment, 
			Map<Element, PresentationFragmentData> presentation_fragments) {
		final Map<Element, Element> presentation_protocols = newHashMap();
		final Map<Element, Element> presentation_implementations = newHashMap();
					
		for (Element element : environment.getElementsAnnotatedWith(Presentation.class)) {
			note("@Presentation is " + element);

            // Skips the current element if it isn't a public interface
            if (verifyPublicInterface(element
                                    , "@Presentation annotation may only be specified on interfaces (%s)."
                                    , "@Presentation interface must be public (%s).")) continue;
			
			// Gathers @Presentation information
            final Map<String, Object> parsed_annotation = getAnnotation(Presentation.class, element);
            final Object protocol_value = parsed_annotation.get("protocol");

            if (protocol_value == null) {
                error(element, "protocol for presentation %s is null.", element);
                // Skips the current element
                continue;
            }

            final Element protocol = getElement(protocol_value);
			
			note("\twith Protocol: " + protocol);

            // Skips the current element if the protocol isn't a public interface
            if (verifyPublicInterface(protocol
                                    , "@Presentation Protocol must be an interface (%s)."
                                    , "@Presentation Protocol must be public (%s).")) continue;

			
			// Adds the mapping of the Presentation to its Protocol (null if no Protocol is defined)
			presentation_protocols.put(element, protocol);
		}

        // Now that the @Presentation annotation has been verified and its data extracted find its implementations
        processPresentationImplementations(environment, presentation_fragments, presentation_protocols, presentation_implementations);

        // TODO: Should require there to exist a @PresentationImplementation for @Controller to work?? Good?
		return ImmutableMap.copyOf(transformEntries(presentation_protocols, 
				new EntryTransformer<Element, Element, PresentationData>() {

					public PresentationData transformEntry(@Nullable Element key, @Nullable Element protocol) { return new PresentationData(protocol, presentation_implementations.get(key)); }
			
		}));
	}

    private void processPresentationImplementations(RoundEnvironment environment
                                                  , Map<Element, PresentationFragmentData> presentation_fragments
                                                  , Map<Element, Element> presentation_protocols
                                                  , Map<Element, Element> presentation_implementations) {
        final TypeMirror presentation_fragment_no_protocol = _element_utilities.getTypeElement(
                PresentationFragment.NoProtocol.class.getCanonicalName()).asType();
        final Set<Element> presentations = newHashSet(presentation_protocols.keySet());
        for (Element implementation_element : environment.getElementsAnnotatedWith(PresentationImplementation.class)) {
            // Makes sure to only deal with Presentation implementations for the current @Presentation
            Element presentation = null;
            for (Element element : presentations)
                if (_type_utilities.isSubtype(implementation_element.asType(), element.asType())) {
                    presentation = element;
                    presentations.remove(presentation);
                    break;
                }
            // Skips the current element
            if (presentation == null) continue;

            note(_WITH_AN_IMPLEMENTATION_OF + implementation_element);

            // Finds Presentation Fragment injections and verifies that their Protocols are met
            verifyPresentationSubProtocols(presentation_fragments, presentation_protocols.get(presentation).asType()
                                         , implementation_element, presentation_fragment_no_protocol);

            // Adds the implementation to the list of imports
            presentation_implementations.put(presentation, implementation_element);
        }
    }

    private void verifyPresentationSubProtocols(Map<Element, PresentationFragmentData> presentation_fragments, TypeMirror protocol
                                              , Element implementation_element, TypeMirror presentation_fragment_no_protocol) {
        for (Element enclosed_element : implementation_element.getEnclosedElements()) {
            if (enclosed_element.getAnnotation(InjectPresentationFragment.class) != null &&
                    enclosed_element.getKind() == FIELD) {
                final Element presentation_fragment = _type_utilities.asElement(enclosed_element.asType());
                // Verifies that the Presentation Fragment is an @PresentationFragment
                if (!presentation_fragments.containsKey(presentation_fragment)) {
                    error(implementation_element,
                            "@InjectPresentationFragment must be an @PresentationFragment %s (%s).",
                            presentation_fragment, implementation_element);
                    continue;
                }

                note("\tcontains Presentation Fragment: " + presentation_fragment);
                // Retrieves the Presentation Fragment's Protocol
                final Element presentation_fragment_protocol =
                        presentation_fragments.get(presentation_fragment).protocol;

                // Verifies that the Presentation Protocol extends the Presentation Fragment Protocol if one is required
                if (presentation_fragment_protocol != null &&
                        !_type_utilities.isSameType(presentation_fragment_no_protocol, presentation_fragment_protocol.asType()) &&
                        !_type_utilities.isSubtype(protocol, presentation_fragment_protocol.asType())) {
                    error(implementation_element,
                            "@Presentation Protocol must extend %s from Presentation Fragment %s (%s.%s).",
                            presentation_fragment_protocol, presentation_fragment, implementation_element, enclosed_element);
                    // Skips the current element
                    continue;
                }
            }
        }
    }

    private void processDataSourceInjections(RoundEnvironment environment,
			                                 List<DataSourceInjectionData> data_source_injections,
			                                 ImmutableMap<Element, PresentationData> presentations,
			                                 ImmutableMap<Element, PresentationFragmentData> presentation_fragments) {
		final TypeMirror no_protocol = _element_utilities.getTypeElement(NoProtocol.class.getCanonicalName()).asType();
        final TypeMirror presentation_fragment_no_protocol = _element_utilities.getTypeElement(
                PresentationFragment.NoProtocol.class.getCanonicalName()).asType();

		for (Element element : environment.getElementsAnnotatedWith(InjectDataSource.class)) {
			final TypeElement enclosing_element = (TypeElement) element.getEnclosingElement();
			note("@InjectDataSource is " + element);
			note(_IN + enclosing_element);
			
			// Verifies containing type is a Presentation or Presentation Fragment Implementation
	        if (enclosing_element.getAnnotation(PresentationImplementation.class) == null &&
	        	enclosing_element.getAnnotation(PresentationFragmentImplementation.class) == null) {
	          error(element, "@InjectDataSource annotations must be specified in @PresentationImplementation or " +
	          		" @PresentationFragmentImplementation-annotated classes (%s).",
	              enclosing_element);
	          // Skips the current element
	          continue;
	        }

	        final boolean is_presentation = enclosing_element.getAnnotation(PresentationFragmentImplementation.class) == null;
            final TypeMirror element_no_protocol = is_presentation? no_protocol : presentation_fragment_no_protocol;
            final TypeMirror protocol = expectedDataSourceProtocol(presentations, presentation_fragments
                                                                 , enclosing_element, is_presentation
                                                                 , element_no_protocol);

            note("\tdefined Protocol is " + protocol);
            if (verifyDataSourceProtocol(element, enclosing_element, is_presentation, element_no_protocol, protocol))
                continue;

            // Verify field properties.
	        Set<Modifier> modifiers = element.getModifiers();
	        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
	          error(element, "@InjectDataSource fields must not be private or static (%s.%s).",
	              enclosing_element.getQualifiedName(), element);
	          continue;
	        }
	        
	        // Gathers the @InjectDataSource information
	        final String package_name = _element_utilities.getPackageOf(enclosing_element) + "";
			final String element_class = _element_utilities.getBinaryName(enclosing_element) + "";
	        data_source_injections.add(new DataSourceInjectionData(package_name, enclosing_element, element, element_class));
		}
	}

    private boolean verifyDataSourceProtocol(Element element, TypeElement enclosing_element, boolean is_presentation
                                           , TypeMirror element_no_protocol, TypeMirror protocol) {
        // Verifies that Presentation/Presentation Fragment has a Protocol
        if (_type_utilities.isSameType(protocol, element_no_protocol)) {
            error(element, "@InjectDataSource may only be used with " +
                  (is_presentation? "Presentations" : "Presentation Fragments") +
                  " that have a Protocol (%s).", enclosing_element);
            // Skips the current element
            return true;
        }
        // Verifies that the target type is the Presentation/Presentation Fragment's Protocol
        if (!_type_utilities.isSameType(element.asType(), protocol)) {
          error(element, "@InjectDataSource fields must be the same as the " +
                  (is_presentation? "Presentation's" : "Presentation Fragment's") +
                  " Protocol, which is %s (%s.%s).", protocol, enclosing_element.getQualifiedName(), element);
          // Skips the current element
            return true;
        }
        return false;
    }

    private TypeMirror expectedDataSourceProtocol(ImmutableMap<Element, PresentationData> presentations
                                                , ImmutableMap<Element, PresentationFragmentData> presentation_fragments
                                                , TypeElement enclosing_element, boolean is_presentation, TypeMirror no_protocol) {
        TypeMirror protocol = no_protocol;
        // Finds the corresponding Presentation Protocol if enclosing element is a Presentation
        if (is_presentation) {
            for (PresentationData data : presentations.values())
                if (data.implementation != null &&
                    _type_utilities.isSameType(data.implementation.asType(), enclosing_element.asType())) {
                    protocol = data.protocol.asType();
                    break;
                }
        // Finds the corresponding Presentation Fragment Protocol if enclosing element is a Presentation Fragment
        } else {
            for (PresentationFragmentData data : presentation_fragments.values())
                if (data.implementation != null &&
                    _type_utilities.isSameType(data.implementation.asType(), enclosing_element.asType())) {
                    protocol = data.protocol.asType();
                    break;
                }
        }
        return protocol;
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
		final TypeMirror default_presentation = _element_utilities.getTypeElement(Default.class.getCanonicalName()).asType();
		final TypeMirror no_protocol = _element_utilities.getTypeElement(NoProtocol.class.getCanonicalName()).asType();
        final Set<Element> controller_interfaces = newHashSet();
		
		for (Element element : environment.getElementsAnnotatedWith(Controller.class)) {
			note("@Controller is " + element);

            // Skips the current element if it isn't a public interface
            if (verifyPublicInterface(element
                                    , "@Controller annotation may only be specified on interfaces (%s)."
                                    , "@Controller interface must be public (%s).")) continue;
			
			// Gathers @Controller information
            final Map<String, Object> parsed_annotation = getAnnotation(Controller.class, element);
            if (parsed_annotation.get("presentation") == null) {
                error(element, "presentation was null for controller %s", element);
                // Skips the current element
                continue;
            }

            final Element presentation = presentationForController(presentations, default_presentation
                                                                 , element, parsed_annotation);
			
			note("\tfor Presentation: " + presentation);
			
			// Verifies that the Controller's Presentation is a Presentation
			if (!presentations.containsKey(presentation)) {
				error(element, "@Controller Presentation must be an @Presentation (%s).", presentation);
				// Skips the current element
				continue;
			}
			
			// Verifies that the Controller implements the Presentation's Protocol, if one is required
			final Element protocol = presentations.get(presentation).protocol;
			if (!(_type_utilities.isSubtype(protocol.asType(), no_protocol) || 
				  _type_utilities.isSubtype(element.asType(), protocol.asType()))) {
				error(element, "@Controller is required to implement Protocol %s by its Presentation (%s).",
					  protocol, presentation);
				// Skips the current element
				continue;
			}
			
			// Adds Presentation Controller binding if Controller has a Presentation implementation
			if (!_type_utilities.isSameType(presentation.asType(), default_presentation)
             && presentations.get(presentation).implementation != null) {
				// TODO: Should require there to exist a @PresentationImplementation for @Controller to work?? Good?
                presentation_controller_bindings.add(new PresentationControllerBinding(element,
							                         presentations.get(presentation).implementation));
                controller_interfaces.add(element);
			}
		}

        // Now that the @Controller annotation has been verified and its data extracted find its implementations
        processControllerImplementations(environment, controllers, controller_interfaces);
    }

    private void processControllerImplementations(RoundEnvironment environment
                                                , Map<String, List<ControllerData>> controllers
                                                , Set<Element> controller_interfaces) {
        for (Element implementation_element : environment.getElementsAnnotatedWith(ControllerImplementation.class)) {
            // Makes sure to only deal with Controller implementations for the current @Controller
            final TypeMirror implementation = implementation_element.asType();
            Element controller = null;
            for (Element element : controller_interfaces)
                if (_type_utilities.isSubtype(implementation, element.asType())) {
                    controller = element;
                    break;
                }
            // Skips the current element
            if (controller == null) continue;

            note(_WITH_AN_IMPLEMENTATION_OF + implementation_element);

            // Gathers @ControllerImplementation information
            final String scope = implementation_element.getAnnotation(ControllerImplementation.class).value();
            final PackageElement package_name = _element_utilities.getPackageOf(implementation_element);
            final List<ControllerData> implementations = controllers.get(scope);
            if (implementations == null)
                controllers.put(scope, newArrayList(new ControllerData(controller, implementation_element)));
                // Verifies that the scope-grouped @ControllerImplementations are in the same package
            else if (!_element_utilities.getPackageOf(implementations.get(0).implementation).equals(package_name)) {
                error(controller, "All @ControllerImplementation(\"%s\") must be defined in the same package (%s).",
                        scope, implementation_element);
                // Skips the current element
                continue;
            } else
                implementations.add(new ControllerData(controller, implementation_element));
        }
    }

    private Element presentationForController(ImmutableMap<Element, PresentationData> presentations
                                            , TypeMirror default_presentation, Element element
                                            , Map<String, Object> parsed_annotation) {
        Element presentation = getElement(parsed_annotation.get("presentation"));

        // Searches for a matching @Presentation if the Presentation is defined by naming convention
        if (_type_utilities.isSameType(presentation.asType(), default_presentation)) {
            final String presentation_from_controller_name =
                    _CONTROLLER_TO_ROOT.reset(element.getSimpleName()+ "").replaceAll("$1Presentation");
            for (Element presentation_interface : presentations.keySet())
                if (presentation_interface.getSimpleName().contentEquals(presentation_from_controller_name)) {
                    presentation = presentation_interface;
                    break;
                }

            // Didn't find a matching @Presentation
            if (_type_utilities.isSameType(presentation.asType(), default_presentation))
                error(element, "No @Presentation-annotated %s found, implicitly required by %s (in %s)"
                    , presentation_from_controller_name, element, Joiner.on('\n').join(presentations.keySet()));
        }
        return presentation;
    }

    @SuppressWarnings("unchecked")
    private void processModels(RoundEnvironment environment, Map<String, List<ModelData>> models,
			                   List<ModelData> model_interfaces) {
        final TypeElement gson_provider = _element_utilities.getTypeElement(GsonProvider.class.getName());
        final Set<Element> elements = (Set<Element>) environment.getElementsAnnotatedWith(Model.class);
        final Map<Element, Boolean> model_should_be_serialized = newHashMap();
        elements.add(gson_provider);
		for (Element element : elements) {
			note("@Model is " + element);

            // Skips the current element if it isn't a public interface
            if (verifyPublicInterface(element
                                    , "@Model annotation may only be specified on interfaces (%s)."
                                    , "@Model interface must be public (%s).")) continue;

            model_should_be_serialized.put(element, false);
		}

        // Now that the @Model annotation has been verified and its data extracted find its implementations
        processModelImplementations(environment, models, model_should_be_serialized);

        // Update model interfaces
        for (Entry<Element, Boolean> entry : model_should_be_serialized.entrySet())
            model_interfaces.add(new ModelData(entry.getKey(), entry.getValue()));
	}

    private void processModelImplementations(RoundEnvironment environment, Map<String, List<ModelData>> models, Map<Element, Boolean> model_should_be_serialized) {
        final Set<Element> model_interfaces = newHashSet(model_should_be_serialized.keySet());
        for (Element implementation_element : environment.getElementsAnnotatedWith(ModelImplementation.class)) {
            // Makes sure to only deal with Model implementations for the current @Model
            final TypeMirror implementation = implementation_element.asType();
            Element model = null;
            for (Element element : model_interfaces)
                if (_type_utilities.isSubtype(implementation, element.asType())) {
                    model = element;
                    break;
                }
            // Skips the current element
            if (model == null) continue;

            note(_WITH_AN_IMPLEMENTATION_OF + implementation_element);

            // Gathers @ModelImplementation information
            final ModelImplementation model_implementation = implementation_element.getAnnotation(ModelImplementation.class);
            final String scope = model_implementation.value();
            final boolean should_serialize = model_implementation.serialize();
            final PackageElement package_name = _element_utilities.getPackageOf(implementation_element);
            final List<ModelData> implementations = models.get(scope);
            final List<? extends VariableElement> parameters = injectModelConstructor((TypeElement) implementation_element);
            if (implementations == null)
                models.put(scope, newArrayList(new ModelData(model, implementation_element, parameters, should_serialize)));
                // Verifies that the scope-grouped @ControllerImplementations are in the same package
            else if (!_element_utilities.getPackageOf(implementations.get(0).implementation).equals(package_name)) {
                error(model, "All @ModelImplementation(\"%s\") must be defined in the same package (%s).",
                        scope, implementation_element);
                // Skips the current element
                continue;
            } else
                implementations.add(new ModelData(model, implementation_element, parameters, should_serialize));

            // TODO: This might be an error instead of a warning, think it crashes if non-serialized model is actually used
            if (!should_serialize && model_should_be_serialized.get(model))
                processingEnv.getMessager().printMessage(WARNING
                        , String.format("Should make all implementations of %s serialized", implementation_element)
                        , implementation_element);
            if (should_serialize) model_should_be_serialized.put(model, true);
        }
    }

    private void processModelInjections(RoundEnvironment environment, Map<Element, List<ModelInjectionData>> model_injections,
			                            List<ModelData> models) {
        final Map<Element, Boolean> model_interfaces = newHashMapWithExpectedSize(models.size());
        for (ModelData model : models) model_interfaces.put(model.contract, model.should_serialize);

		for (Element element : environment.getElementsAnnotatedWith(InjectModel.class)) {
			// @InjectModel constructors are processed during @Model processing
			if (element.getKind() != FIELD) continue;
			
			final TypeElement enclosing_element = (TypeElement) element
					.getEnclosingElement();
			note("@InjectModel is " + element);
			note(_IN + enclosing_element);

            // Verifies containing type is not a Presentation Implementation
            if (enclosing_element.getAnnotation(PresentationImplementation.class) != null) {
                error(element,
                        "@InjectModel-annotated fields must not be specified in @PresentationImplementation classes (%s).",
                        enclosing_element);
                // Skips the current element
                continue;
            }

            // Verifies containing type is not a Presentation Fragment Implementation
            if (enclosing_element.getAnnotation(PresentationFragmentImplementation.class) != null) {
                error(element,
                        "@InjectModel-annotated fields must not be specified in @PresentationFragmentImplementation classes (%s).",
                        enclosing_element);
                // Skips the current element
                continue;
            }

            final Element type_element = _type_utilities.asElement(element.asType());
			// Verifies that the target type is a Model
			note("\tinjecting Model " + element.asType());
			if (!model_interfaces.containsKey(type_element)) {
				error(element, "@InjectModel must be a Model (%s.%s)."
                    , enclosing_element.getQualifiedName(), element);
				continue;
			}

			// Verifies field properties
			Set<Modifier> modifiers = element.getModifiers();
			if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
				error(element, "@InjectModel fields must not be private or static (%s.%s).",
					  enclosing_element.getQualifiedName(), element);
				continue;
			}

			// Gathers the @InjectModel information
			final String package_name = _element_utilities.getPackageOf(enclosing_element) + "";
			final String element_class = _element_utilities.getBinaryName(enclosing_element) + "";
			final ModelInjectionData injection = new ModelInjectionData(package_name, element, element_class
                                                                      , model_interfaces.get(type_element));
			if (model_injections.containsKey(enclosing_element))
				model_injections.get(enclosing_element).add(injection);
			else
				model_injections.put(enclosing_element, newArrayList(injection));
		}
	}

	private void processPresentationFragmentInjections(RoundEnvironment environment,
            Map<Element, List<PresentationFragmentInjectionData>> presentation_fragment_injections,
            Map<Element, List<PresentationFragmentInjectionData>> controller_presentation_fragment_injections,
            Map<Element, PresentationFragmentData> presentation_fragments) {
		for (Element element : environment.getElementsAnnotatedWith(InjectPresentationFragment.class)) {
			// Verifies @InjectPresentationFragment is on a field
			if (element.getKind() != FIELD)
				continue;

			final TypeElement enclosing_element = (TypeElement) element
					.getEnclosingElement();
			note("@InjectPresentationFragment is " + element);
			note(_IN + enclosing_element);

			// Verifies containing type is a Presentation, Presentation Fragment, or Controller implementation
			if (enclosing_element.getAnnotation(PresentationImplementation.class) == null && 
				enclosing_element.getAnnotation(PresentationFragmentImplementation.class) == null &&
				enclosing_element.getAnnotation(ControllerImplementation.class) == null) {
				error(element,
						"@InjectPresentationFragment-annotated fields must be specified in @PresentationImplementation " +
						"or @PresentationFragmentImplementation or @ControllerImplementation classes (%s.%s).",
						enclosing_element.getQualifiedName(), element);
				// Skips the current element
				continue;
			}

            // Verifies type is a Presentation Fragment
            if (!presentation_fragments.containsKey(_type_utilities.asElement(element.asType()))) {
                error(element, "@InjectPresentationFragment must be a @PresentationFragment (%s.%s) in (%s)"
                    , enclosing_element, element, presentation_fragments.keySet());
                // Skips the current element
                continue;
            }

			// Verifies field properties
			Set<Modifier> modifiers = element.getModifiers();
			if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
				error(element,
						"@InjectPresentationFragment fields must not be private or static (%s.%s).",
						enclosing_element.getQualifiedName(), element);
				continue;
			}

			// Gathers the @InjectPresentationFragment information
			final String package_name = _element_utilities.getPackageOf(enclosing_element) + "";
			final String element_class = _element_utilities.getBinaryName(enclosing_element) + "";
			final InjectPresentationFragment inject_annotation = 
					element.getAnnotation(InjectPresentationFragment.class);
			final int[] displays = inject_annotation.value();
			final String tag = inject_annotation.tag();
			final boolean is_manually_created = inject_annotation.manual();
			final PresentationFragmentInjectionData injection = new PresentationFragmentInjectionData(
					package_name, element, element_class, displays, tag, 
					presentation_fragments.get(_type_utilities.asElement(element.asType())).implementation,
					is_manually_created);
            addPresentationFragmentInjection(presentation_fragment_injections
                                           , controller_presentation_fragment_injections
                                           , enclosing_element, injection);
        }
	}

    private static void addPresentationFragmentInjection(Map<Element, List<PresentationFragmentInjectionData>> presentation_fragment_injections
                                                       , Map<Element, List<PresentationFragmentInjectionData>> controller_presentation_fragment_injections
                                                       , TypeElement enclosing_element, PresentationFragmentInjectionData injection) {
        if (enclosing_element.getAnnotation(ControllerImplementation.class) != null) {
            if (controller_presentation_fragment_injections.containsKey(enclosing_element))
                controller_presentation_fragment_injections.get(enclosing_element).add(injection);
            else
                controller_presentation_fragment_injections.put(enclosing_element, newArrayList(injection));
        } else if (presentation_fragment_injections.containsKey(enclosing_element))
            presentation_fragment_injections.get(enclosing_element).add(injection);
        else
            presentation_fragment_injections.put(enclosing_element, newArrayList(injection));
    }

    // TODO: add tests for @InjectPresentation
    private void processPresentationInjections(RoundEnvironment environment,
                                               Map<Element, PresentationInjectionData> controller_presentation_injections) {
        for (Element element : environment.getElementsAnnotatedWith(InjectPresentation.class)) {
            // Verifies @InjectPresentation is on a field
            if (element.getKind() != FIELD)
                continue;

            final TypeElement enclosing_element = (TypeElement) element.getEnclosingElement();
            note("@InjectPresentation is " + element);
            note(_IN + enclosing_element);

            // Verifies containing type is a Controller implementation
            if (enclosing_element.getAnnotation(ControllerImplementation.class) == null) {
                error(element,
                        "@InjectPresentation-annotated fields must be specified in @ControllerImplementation " +
                                " classes (%s).",
                        enclosing_element);
                // Skips the current element
                continue;
            }

            // Verifies field properties
            Set<Modifier> modifiers = element.getModifiers();
            if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
                error(element,
                        "@InjectPresentation fields must not be private or static (%s.%s).",
                        enclosing_element.getQualifiedName(), element);
                continue;
            }

            // Gathers the @InjectPresentation information
            final String package_name = _element_utilities.getPackageOf(enclosing_element) + "";
            final String element_class = _element_utilities.getBinaryName(enclosing_element) + "";
            final PresentationInjectionData injection = new PresentationInjectionData(package_name, element, element_class);

            // Verifies only one @InjectPresentation per Controller
            if (controller_presentation_injections.containsKey(enclosing_element)) {
                error(element,
                        "A Controller must only have one @InjectPresentation-annotated field " +
                                " (%s).",
                        enclosing_element);
                // Skips the current element
                //noinspection UnnecessaryContinue
                continue;
            } else
                controller_presentation_injections.put(enclosing_element, injection);
        }
    }
	
	@CheckForNull private List<? extends VariableElement> injectModelConstructor(TypeElement element) {
		List<? extends VariableElement> parameters = null;
		boolean found_accessible_constructor = false;
		boolean found_annotated_constructor = false;
		for (Element member : element.getEnclosedElements())
			if (member.getAnnotation(InjectModel.class) != null && member.getKind() == CONSTRUCTOR) {
				// Verifies there is at most one @InjectModel constructors
				if (found_annotated_constructor)
					error(element, "There must only be one @InjectModel constructor (%s).", member);
				parameters = ((ExecutableElement) member).getParameters();
                found_annotated_constructor = true;
				found_accessible_constructor = !member.getModifiers().contains(PRIVATE);
			} else if (member.getKind() == CONSTRUCTOR)
				found_accessible_constructor = !member.getModifiers().contains(PRIVATE);
		
		// Verifies there is an accessible constructor 
		if (!found_accessible_constructor)
			error(element, "There must be a non-private @InjectModel or default constructor (%s).", element);
		return parameters;
	}

    /**
     * Retrieves an {@link javax.lang.model.element.Element} from {@code some_class_mirror}.
     * @param some_class_mirror Some object that should represent a {@link Class} in some form
     * @return The element for the represented class
     */
    private Element getElement(Object some_class_mirror) {
        return some_class_mirror instanceof Class
                ? _element_utilities.getTypeElement(((Class) some_class_mirror).getCanonicalName())
                :  _type_utilities.asElement((TypeMirror) some_class_mirror);
    }

	/**
	 * <p>Produces an error message with the given information.</p>
	 * @param element The element to relate the error message to
	 * @param message The message to send
	 * @param arguments Arguments to format into the message
	 */
	private void error(Element element, String message, Object... arguments) {
		processingEnv.getMessager().printMessage(ERROR, String.format(message, arguments), element);
	}

    /**
     * <p>Produces an note message with the given information.</p>
     * @param message The message to send
     * @param arguments Arguments to format into the message
     */
    private void note(String message, Object... arguments) {
        processingEnv.getMessager().printMessage(NOTE, String.format(message, arguments));
    }

  /** Extracts the root from a Controller following the naming convention "*Controller" */
	private static final Matcher _CONTROLLER_TO_ROOT = Pattern.compile("(.+)Controller").matcher("");
  private static final String _WITH_AN_IMPLEMENTATION_OF = "\twith an implementation of ";
  private static final String _IN = "\tin ";
	/** Counts the number of passes The Annotation Processor has performed */
	private int _passes = 0;
	private Elements _element_utilities;
	private Types _type_utilities;
}