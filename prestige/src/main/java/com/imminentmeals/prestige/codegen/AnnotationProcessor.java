package com.imminentmeals.prestige.codegen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.imminentmeals.prestige.ControllerContract;
import com.imminentmeals.prestige.GsonProvider;
import com.imminentmeals.prestige.Prestige;
import com.imminentmeals.prestige.SegueController;
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
import com.imminentmeals.prestige.annotations.meta.Implementations;
import com.squareup.javawriter.JavaWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
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
import javax.tools.JavaFileObject;

import dagger.Lazy;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import timber.log.Timber;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Maps.transformEntries;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;
import static com.imminentmeals.prestige.codegen.Utilities.getAnnotation;
import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static java.lang.Math.min;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;

// TODO: can you mandate that a default implementation is provided for everything that has any implementation?
// TODO: public static Strings for generated methods and classes to use in Prestige helper methods
// TODO: generate ExclusionPolicy for Model fields to be skipped by Gson
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
			final Element implementation = controller_implementations.getValue().get(0)._implementation;
			final String package_name = _element_utilities.getPackageOf(implementation).getQualifiedName() + "";
			final String class_name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, 
					controller_implementations.getKey()) + CONTROLLER_MODULE_SUFFIX;
			controller_modules.add(new ModuleData(String.format("%s.%s", package_name, class_name), 
					                              controller_implementations.getKey(),class_name, package_name,
					                              controller_implementations.getValue()));
		}
		final ImmutableList.Builder<ModuleData> model_modules = ImmutableList.builder();
		for (Map.Entry<String, List<ModelData>> model_implementations : models.entrySet()) {
			final Element implementation = model_implementations.getValue().get(0)._implementation;
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
							if (presentation_fragment._is_manually_created)
								// Skips the current injection, since it will be handled manually
								continue;
							for (Entry<Integer, Integer> injection : presentation_fragment._displays.entrySet())
								if (injections.containsKey(injection.getKey()))
									injections.get(injection.getKey()).add(presentation_fragment);
								else
									injections.put(injection.getKey(), newArrayList(presentation_fragment));
						}
						return ImmutableMap.copyOf(injections);
					}
				});
        final ImmutableMap.Builder<Element, ModelData> element_to_model_map = ImmutableMap.builder();
        for (ModelData model : model_interfaces) element_to_model_map.put(model._interface, model);
        final Map<String, Map<Element, ModelData>> model_implementations = newHashMap();
        final int number_of_scopes = models.size();
        for (Entry<String, List<ModelData>> entry : models.entrySet()) {
            Map<Element, ModelData> scoped_models;
            if ((scoped_models = model_implementations.get(entry.getKey())) == null) {
                scoped_models = newHashMapWithExpectedSize(number_of_scopes);
                model_implementations.put(entry.getKey(), scoped_models);
            }
            for (ModelData implementation : entry.getValue())
                scoped_models.put(implementation._interface, implementation);
        }
		
		// Generates the code
		generateSourceCode(presentation_controller_bindings, controller_modules.build(), data_source_injections,
				           model_modules.build(), model_interfaces, element_to_model_map.build(),
                           ImmutableMap.copyOf(model_implementations), model_injections,
				           ImmutableMap.copyOf(presentation_fragment_display_injections),
				           ImmutableMap.copyOf(controller_presentation_fragment_injections), controller_presentation_injections);
	     
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
		final TypeMirror fragment_type = _element_utilities.getTypeElement(Fragment.class.getCanonicalName()).asType();
		final Map<Element, Element> presentation_fragment_protocols = newHashMap();
		final Map<Element, Element> presentation_fragment_implementations = newHashMap();
		final Map<Element, Set<Element>> unverified_presentation_fragments = newHashMap();
		final TypeMirror no_protocol = _element_utilities.getTypeElement(PresentationFragment.NoProtocol.class.getCanonicalName()).asType();

		for (Element element : environment.getElementsAnnotatedWith(PresentationFragment.class)) {
			note(element, "@PresentationFragment is " + element);
			
			// Verifies that the target type is an interface
			if (element.getKind() != INTERFACE) {
				error(element, "@PresentationFragment annotation may only be specified on interfaces (%s).", element);
				// Skips the current element
				continue;
			}
			
			// Verifies that the interface's visibility is public
			if (!element.getModifiers().contains(PUBLIC)) {
				error(element, "@PresentationFragment interfaces must be public (%s).", element);
				// Skips the current element
				continue;
			}

			// Gathers @PresentationFragment information
            final Map<String, Object> parsed_annotation = getAnnotation(PresentationFragment.class, element);

            if (parsed_annotation.get("protocol") == null) {
                error(element, "protocol for presentation %s is null.", element);
                // Skips the current element
                continue;
            }

            final Element protocol = getElement(parsed_annotation.get("protocol"));

            note(element, "\twith Protocol: " + protocol);
			
			// Verifies that the Protocol is an Interface
			if (protocol.getKind() != INTERFACE) {
				error(element, "@PresentationFragment Protocol must be an interface (%s).",
					  protocol);
				// Skips the current element
				continue;
			}
			
			// Verifies that the Protocol visibility is public
			if (!protocol.getModifiers().contains(PUBLIC)) {
				error(element, "@PresentationFragment Protocol must be public (%s).",
						protocol);
				// Skips the current element
				continue;
			}

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
			
			// Adds the mapping of the Presentation to its Protocol
			presentation_fragment_protocols.put(element, protocol);
			
			// Now that the @PresentationFragment annotation has been verified and its data extracted find its implementations				
			// TODO: very inefficient
			for (Element implementation_element : environment.getElementsAnnotatedWith(PresentationFragmentImplementation.class)) {
				// Makes sure to only deal with Presentation Fragment implementations for the current @PresentationFragment
				if (!_type_utilities.isSubtype(implementation_element.asType(), element.asType()))
					continue;
				
				note(implementation_element, "\twith an implementation of " + implementation_element);
				
				// Verifies that the Presentation Fragment implementation extends from Fragment
		        if (!_type_utilities.isSubtype(implementation_element.asType(), fragment_type)) {
		          error(implementation_element, "@PresentationFragmentImplementation classes must extend from Fragment (%s).",
		                implementation_element); 
		          // Skips the current element
		          continue;
		        }
		        
		        // Finds Presentation Fragment injections and verifies that their Protocols are met
		        for (Element enclosed_element : implementation_element.getEnclosedElements()) {
		        	if (enclosed_element.getAnnotation(InjectPresentationFragment.class) != null &&
		        		enclosed_element.getKind() == FIELD) {			
		    			// Verifies that the Protocol extends all of the Presentation Fragment's Protocols
		    			// Notice that it only checks against the Presentation Fragments that have already been processed
		        		final Element sub_presentation_fragment = _type_utilities.asElement(enclosed_element.asType());
		        		note(implementation_element, "\tcontains Presentation Fragment: " + sub_presentation_fragment);
	    				if (!_type_utilities.isSameType(presentation_fragment_protocols.get(sub_presentation_fragment).asType(), no_protocol)) {
	    					final TypeMirror sub_protocol = 
	    							presentation_fragment_protocols.get(sub_presentation_fragment).asType();
	    					if (!_type_utilities.isSubtype(protocol.asType(), sub_protocol)) {
	    						error(implementation_element, 
	    							  "@PresentationFragment Protocol must extend %s from Presentation Fragment %s (%s).",
	    							  sub_protocol, sub_presentation_fragment, implementation_element);
	    						// Skips the current element
	    						continue;
	    					} 
	    				} else if (unverified_presentation_fragments.containsKey(enclosed_element))
	    					unverified_presentation_fragments.get(enclosed_element).add(element);
	    				else
	    					unverified_presentation_fragments.put(enclosed_element, newHashSet(element));
		        	}
		        }
		        
		        // Adds the implementation to the list of imports
				presentation_fragment_implementations.put(element, implementation_element);
			}
		}
		
		// Verifies that all PresentationFragments have been verified
		final String format = "@PresentationFragment Protocol must extend %s from Presentation Fragment %s (%s).";
		for (Entry<Element, Set<Element>> entry : unverified_presentation_fragments.entrySet()) {
			final Element protocol = presentation_fragment_protocols.get(entry.getKey());
            if (protocol != null)
			    for (Element element : entry.getValue())
				    error(element, format, protocol, entry.getKey(), element);
		}
		
		return ImmutableMap.copyOf(transformEntries(presentation_fragment_protocols,
				new EntryTransformer<Element, Element, PresentationFragmentData>() {

					public PresentationFragmentData transformEntry(@Nonnull Element key, @Nullable Element protocol) { return new PresentationFragmentData(protocol == null? _type_utilities.asElement(no_protocol) : protocol
                                                                                          , presentation_fragment_implementations.get(key)); }
			
		}));
	}

	/**
	 * <p>Processes the source code for the @Presentation and @PresentationImplementation annotations.</p>
	 * @param environment The round environment
	 */
	private ImmutableMap<Element, PresentationData> processPresentations(RoundEnvironment environment, 
			Map<Element, PresentationFragmentData> presentation_fragments) {
		final TypeMirror activity_type = _element_utilities.getTypeElement(Activity.class.getCanonicalName()).asType();
		final Map<Element, Element> presentation_protocols = newHashMap();
		final Map<Element, Element> presentation_implementations = newHashMap();
					
		for (Element element : environment.getElementsAnnotatedWith(Presentation.class)) {
			note(element, "@Presentation is " + element);
			
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
            final Map<String, Object> parsed_annotation = getAnnotation(Presentation.class, element);
            final Object protocol_value = parsed_annotation.get("protocol");

            if (protocol_value == null) {
                error(element, "protocol for presentation %s is null.", element);
                // Skips the current element
                continue;
            }

            final Element protocol = getElement(protocol_value);
			
			note(element, "\twith Protocol: " + protocol);
			
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
				
				note(element, "\twith an implementation of " + implementation_element);
				
				// Verifies that the Presentation implementation extends from Activity
		        if (!_type_utilities.isSubtype(implementation_element.asType(), activity_type)) {
		          error(element, "@PresentationImplementation classes must extend from Activity (%s).",
		                implementation_element);
		          // Skips the current element
		          continue;
		        }

                final TypeMirror presentation_fragment_no_protocol = _element_utilities.getTypeElement(
                        PresentationFragment.NoProtocol.class.getCanonicalName()).asType();
		        // Finds Presentation Fragment injections and verifies that their Protocols are met
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
		        		
		        		note(implementation_element, "\tcontains Presentation Fragment: " + presentation_fragment);
		        		// Retrieves the Presentation Fragment's Protocol
		        		final Element presentation_fragment_protocol = 
		        				presentation_fragments.get(presentation_fragment).protocol;
		        		
		        		// Verifies that the Presentation Protocol extends the Presentation Fragment Protocol if one is required
		        		if (presentation_fragment_protocol != null &&
                            !_type_utilities.isSameType(presentation_fragment_no_protocol, presentation_fragment_protocol.asType()) &&
		        			!_type_utilities.isSubtype(protocol.asType(), presentation_fragment_protocol.asType())) {
    						error(implementation_element, 
    							  "@Presentation Protocol must extend %s from Presentation Fragment %s (%s.%s).",
    							  presentation_fragment_protocol, presentation_fragment, implementation_element, enclosed_element);
    						// Skips the current element
    						continue;
	    				}
		        	}
		        }
		        
		        // Adds the implementation to the list of imports
				presentation_implementations.put(element, implementation_element);
			}
		}
		
		// TODO: Should require there to exist a @PresentationImplementation for @Controller to work?? Good?
		return ImmutableMap.copyOf(transformEntries(presentation_protocols, 
				new EntryTransformer<Element, Element, PresentationData>() {

					public PresentationData transformEntry(@Nonnull Element key, @Nullable Element protocol) { return new PresentationData(protocol, presentation_implementations.get(key)); }
			
		}));
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
			note(element, "@InjectDataSource is " + element);
			note(element, "\tin " + enclosing_element);
			
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
            TypeMirror protocol = element_no_protocol;
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
	        note(element, "\tdefined Protocol is " + protocol);
	        // Verifies that Presentation/Presentation Fragment has a Protocol
	        if (_type_utilities.isSameType(protocol, element_no_protocol)) {
	        	error(element, "@InjectDataSource may only be used with " + 
	                  (is_presentation? "Presentations" : "Presentation Fragments") + 
	        		  " that have a Protocol (%s).", enclosing_element);
	        	// Skips the current element
	        	continue;
	        }
	        // Verifies that the target type is the Presentation/Presentation Fragment's Protocol
	        if (!_type_utilities.isSameType(element.asType(), protocol)) {
	          error(element, "@InjectDataSource fields must be the same as the " +
	          		(is_presentation? "Presentation's" : "Presentation Fragment's") +
	          		" Protocol, which is %s (%s.%s).", protocol, enclosing_element.getQualifiedName(), element);
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
			final String element_class = _element_utilities.getBinaryName(enclosing_element) + "";
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
		final TypeMirror default_presentation = _element_utilities.getTypeElement(Default.class.getCanonicalName()).asType();
		final TypeMirror no_protocol = _element_utilities.getTypeElement(NoProtocol.class.getCanonicalName()).asType();
		
		for (Element element : environment.getElementsAnnotatedWith(Controller.class)) {
			note(element, "@Controller is " + element);
			
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
			
			// Gathers @Controller information
            final Map<String, Object> parsed_annotation = getAnnotation(Controller.class, element);
            if (parsed_annotation.get("presentation") == null) {
                error(element, "presentation was null for controller %s", element);
                // Skips the current element
                continue;
            }

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
			
			note(element, "\tfor Presentation: " + presentation);
			
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
			
			// Adds Presentation Controller binding if Controller has a Presentation
			if (!_type_utilities.isSameType(presentation.asType(), default_presentation)
             && presentations.get(presentation).implementation != null) {
				// TODO: Should require there to exist a @PresentationImplementation for @Controller to work?? Good?
                presentation_controller_bindings.add(new PresentationControllerBinding(element,
							                         presentations.get(presentation).implementation));
			}
			
			// Now that the @Controller annotation has been verified and its data extracted find its implementations				
			// TODO: very inefficient
			for (Element implementation_element : environment.getElementsAnnotatedWith(ControllerImplementation.class)) {
				// Makes sure to only deal with Controller implementations for the current @Controller
				if (!_type_utilities.isSubtype(implementation_element.asType(), element.asType()))
					continue;
				
				note(element, "\twith an implementation of " + implementation_element);
				
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
	}
	
	@SuppressWarnings("unchecked")
    private void processModels(RoundEnvironment environment, Map<String, List<ModelData>> models,
			                   List<ModelData> model_interfaces) {
        final TypeElement gson_provider = _element_utilities.getTypeElement(GsonProvider.class.getName());
        final Set<Element> elements = (Set<Element>) environment.getElementsAnnotatedWith(Model.class);
        final Map<Element, Boolean> model_should_be_serialized = newHashMap();
        elements.add(gson_provider);
		for (Element element : elements) {
			note(element, "@Model is " + element);
			
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

            model_should_be_serialized.put(element, false);
			// Now that the @Controller annotation has been verified and its data extracted find its implementations
			// TODO: very inefficient
			for (Element implementation_element : environment.getElementsAnnotatedWith(ModelImplementation.class)) {
				// Makes sure to only deal with Model implementations for the current @Model
				if (!_type_utilities.isSubtype(implementation_element.asType(), element.asType()))
					continue;
				
				note(element, "\twith an implementation of " + implementation_element);
				
				// Gathers @ModelImplementation information
                final ModelImplementation model_implementation = implementation_element.getAnnotation(ModelImplementation.class);
				final String scope = model_implementation.value();
                final boolean should_serialize = model_implementation.serialize();
                final PackageElement package_name = _element_utilities.getPackageOf(implementation_element);
				final List<ModelData> implementations = models.get(scope);
				final List<? extends VariableElement> parameters = injectModelConstructor((TypeElement) implementation_element);
				if (implementations == null)
					models.put(scope, newArrayList(new ModelData(element, implementation_element, parameters, should_serialize)));
				// Verifies that the scope-grouped @ControllerImplementations are in the same package
				else if (!_element_utilities.getPackageOf(implementations.get(0)._implementation).equals(package_name)) {
					error(element, "All @ModelImplementation(\"%s\") must be defined in the same package (%s).",
						  scope, implementation_element);
					// Skips the current element
					continue;
				} else
					implementations.add(new ModelData(element, implementation_element, parameters, should_serialize));

                if (!should_serialize && model_should_be_serialized.get(element))
                    processingEnv.getMessager().printMessage(WARNING
                                                           , String.format("Should make all implementations of %s serialized", implementation_element)
                                                           , implementation_element);
                if (should_serialize) model_should_be_serialized.put(element, true);
			}
		}

        // Update model interfaces
        for (Entry<Element, Boolean> entry : model_should_be_serialized.entrySet())
            model_interfaces.add(new ModelData(entry.getKey(), entry.getValue()));
	}
	
	private void processModelInjections(RoundEnvironment environment, Map<Element, List<ModelInjectionData>> model_injections,
			                            List<ModelData> models) {
        final Map<Element, Boolean> model_interfaces = newHashMapWithExpectedSize(models.size());
        for (ModelData model : models) model_interfaces.put(model._interface, model._should_serialize);

		for (Element element : environment.getElementsAnnotatedWith(InjectModel.class)) {
			// @InjectModel constructors are processed during @Model processing
			if (element.getKind() != FIELD) continue;
			
			final TypeElement enclosing_element = (TypeElement) element
					.getEnclosingElement();
			note(element, "@InjectModel is " + element);
			note(element, "\tin " + enclosing_element);

            // TODO: @InjectModel is allowed in unannotated classes now, provide support for such use
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
			note(element, "\tinjecting Model " + element.asType());
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
			note(element, "@InjectPresentationFragment is " + element);
			note(element, "\tin " + enclosing_element);

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
	}

	// TODO: add tests for @InjectPresentation
    private void processPresentationInjections(RoundEnvironment environment,
                                               Map<Element, PresentationInjectionData> controller_presentation_injections) {
        for (Element element : environment.getElementsAnnotatedWith(InjectPresentation.class)) {
            // Verifies @InjectPresentation is on a field
            if (element.getKind() != FIELD)
                continue;

            final TypeElement enclosing_element = (TypeElement) element.getEnclosingElement();
            note(element, "@InjectPresentation is " + element);
            note(element, "\tin " + enclosing_element);

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

	@SuppressWarnings("unchecked")
	private void generateSourceCode(List<PresentationControllerBinding> controllers, List<ModuleData> controller_modules,
			                        List<DataSourceInjectionData> data_source_injections,
			                        List<ModuleData> model_modules, List<ModelData> model_interfaces,
                                    Map<Element, ModelData> element_to_model_interface,
                                    Map<String, Map<Element, ModelData>> model_implementations,
			                        Map<Element, List<ModelInjectionData>> model_injections,
			                        Map<Element, Map<Integer, List<PresentationFragmentInjectionData>>> presentation_fragment_injections,
			                        Map<Element, List<PresentationFragmentInjectionData>> controller_presentation_fragment_injections,
                                    Map<Element, PresentationInjectionData> controller_presentation_injections) {
		final Filer filer = processingEnv.getFiler();
		Writer writer = null;
		try {				
			// Generates the _SegueController
			JavaFileObject source_code = filer.createSourceFile(_SEGUE_CONTROLLER_SOURCE, (Element) null);
	        writer = source_code.openWriter();
	        writer.flush();
	        generateSegueControllerSourceCode(writer, controllers, controller_modules, model_modules, model_interfaces
                                            , element_to_model_interface, model_implementations);
	        
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
	        for (Entry<Element, List<ModelInjectionData>> injection : model_injections.entrySet()) {
	        	final TypeElement element = (TypeElement) injection.getKey();
	        	final String full_name = element_utilities.getBinaryName(element) + MODEL_INJECTOR_SUFFIX;
	        	source_code = filer.createSourceFile(full_name, element);
	        	writer = source_code.openWriter();
	        	writer.flush();
	        	final String package_name = _element_utilities.getPackageOf(element) + "";
	        	final String class_name = full_name.substring(package_name.length() + 1);
	        	generateModelInjector(writer, package_name, injection.getValue(), class_name, element);
	        }
	        
	        // Generates the $$PresentationFragmentInjectors
	        for (Entry<Element, Map<Integer, List<PresentationFragmentInjectionData>>> injection : presentation_fragment_injections.entrySet()) {
	        	final TypeElement element = (TypeElement) injection.getKey();
	        	final String full_name = _element_utilities.getBinaryName(element) + PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;
	        	source_code = filer.createSourceFile(full_name, element);
	        	writer = source_code.openWriter();
	        	writer.flush();
	        	final String package_name = _element_utilities.getPackageOf(element) + "";
	        	final String class_name = full_name.substring(package_name.length() + 1);
	        	generatePresentationFragmentInjector(writer, package_name, injection.getValue(), class_name, element);
	        }
	        for (Entry<Element, List<PresentationFragmentInjectionData>> injection : controller_presentation_fragment_injections.entrySet()) {
	        	final TypeElement element = (TypeElement) injection.getKey();
	        	final String full_name = _element_utilities.getBinaryName(element) + PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;
	        	source_code = filer.createSourceFile(full_name, element);
	        	writer = source_code.openWriter();
	        	writer.flush();
	        	final String package_name = _element_utilities.getPackageOf(element) + "";
	        	final String class_name = full_name.substring(package_name.length() + 1);
	        	generateControllerPresentationFragmentInjector(writer, package_name, injection.getValue(), class_name, element);
	        }

            // Generates the $$PresentationInjectors
            for (Entry<Element, PresentationInjectionData> injection : controller_presentation_injections.entrySet()) {
                final TypeElement element = (TypeElement) injection.getKey();
                final String full_name = _element_utilities.getBinaryName(element) + PRESENTATION_INJECTOR_SUFFIX;
                source_code = filer.createSourceFile(full_name, element);
                writer = source_code.openWriter();
                writer.flush();
                final String package_name = _element_utilities.getPackageOf(element) + "";
                final String class_name = full_name.substring(package_name.length() + 1);
                generateControllerPresentationInjector(writer, package_name, injection.getValue(), class_name, element);
            }
		} catch (IOException exception) {
			processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
		} finally {
			try {
				Closeables.close(writer, writer != null);
			} catch (IOException exception) {
                processingEnv.getMessager().printMessage(ERROR, exception.getMessage());
            }
		}
	}

	/**
	 *
     * @param writer
     * @param models
     * @throws IOException
	 */
	private void generateSegueControllerSourceCode(Writer writer, List<PresentationControllerBinding> controllers,
                                                   List<ModuleData> controller_modules, List<ModuleData> model_modules,
                                                   List<ModelData> models, Map<Element, ModelData> element_to_model_interfaces,
                                                   Map<String, Map<Element, ModelData>> model_implementations)
			                                        		throws IOException {
		final EnumSet<Modifier> public_modifier = EnumSet.of(PUBLIC);
		final EnumSet<Modifier> protected_modifier = EnumSet.of(PROTECTED);
        final String else_if = "else if";
        String if_else_if_control;
		JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
		java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
				   .emitPackage("com.imminentmeals.prestige")
		           .emitImports(JavaWriter.type(ImmutableMap.class)
                           , JavaWriter.type(GsonConverter.class)
                           , JavaWriter.type(IOException.class)
                           , JavaWriter.type(ArrayList.class)
                           , JavaWriter.type(List.class)
                           , JavaWriter.type(Nonnull.class)
                           , JavaWriter.type(Inject.class)
                           , JavaWriter.type(Named.class)
                           , JavaWriter.type(Provider.class)
                           , JavaWriter.type(Lazy.class)
                           , JavaWriter.type(ObjectGraph.class)
                           , JavaWriter.type(Timber.class))
                   .emitEmptyLine()
                   .emitStaticImports(JavaWriter.type(Prestige.class) + "._TAG")
				   .emitEmptyLine()
				   .emitJavadoc("<p>A Segue Controller that handles getting the appropriate Controller\n" +
                           "for the current Presentation, and communicating with the Controller Bus.</p>")
				   .beginType("com.imminentmeals.prestige._SegueController", "class", public_modifier
                           // extends
                           , JavaWriter.type(SegueController.class));
		final StringBuilder controller_puts = new StringBuilder();
		for (PresentationControllerBinding binding : controllers) {
			java_writer.emitJavadoc("Provider for instances of the {@link %s} Controller", binding._controller)
			           .emitAnnotation(Inject.class)
			           .emitField("javax.inject.Provider<" + binding._controller + ">", binding._variable_name);
			controller_puts.append(String.format(".put(%s.class, %s)%n",
					    binding._presentation_implementation, binding._variable_name));
		}
		
		final StringBuilder model_puts = new StringBuilder();
		for (ModelData model : models) {
			java_writer.emitJavadoc("Provider for instances of the {@link %s} Model", model._interface)
			           .emitAnnotation(Inject.class)
			           .emitField("dagger.Lazy<" + model._interface + ">", model._variable_name);
			model_puts.append(String.format(".put(%s.class, %s)%n",
                    model._interface, model._variable_name));
            if (model._should_serialize)
                java_writer.emitJavadoc("Provider for {@link %s} for {@link %s}", JavaWriter.type(GsonConverter.class),
                                        model._interface)
                           .emitAnnotation(Inject.class)
                           .emitAnnotation(Named.class, stringLiteral(model._interface + ""))
                           .emitField(JavaWriter.type(Lazy.class, JavaWriter.type(GsonConverter.class)),
                                   model._variable_name + _CONVERTOR);
		}
		// Constructor
		java_writer.emitEmptyLine()
		           .emitJavadoc("<p>Constructs a {@link SegueController}.</p>")
		           .beginMethod(null, "com.imminentmeals.prestige._SegueController", public_modifier,
                           JavaWriter.type(String.class), "scope",
                           JavaWriter.type(Timber.class), "log")
                   .emitStatement("super(scope, log)")
				   .endMethod()
				   .emitEmptyLine()
				   // SegueController Contract
                   .emitAnnotation(Override.class)
                   .beginMethod("<T> void", "store", public_modifier, newArrayList("T", "object")
                           , newArrayList(JavaWriter.type(IOException.class)));
        if_else_if_control = "if";
        String nested_if_else_if;
        for (ModelData model : models) {
            if (!model._should_serialize) continue;

            nested_if_else_if = "if";
            java_writer.beginControlFlow(String.format("%s (object instanceof %s)", if_else_if_control, model._interface))
                       .emitStatement("_log.tag(_TAG).d(\"Storing \" + object)")
                       .emitStatement("%s%s.get().toStream(object, gson_provider.get().outputStreamFor(%s.class))"
                               , model._variable_name, _CONVERTOR, model._interface);

            // TODO: avoid storing the same model multiple times in one pass
            for (Entry<String, Map<Element, ModelData>> entry : model_implementations.entrySet()) {
                ModelData model_implementation = entry.getValue().get(model._interface);
                if (model_implementation == null)
                    model_implementation = model_implementations.containsKey(PRODUCTION)? model_implementations.get(PRODUCTION).get(model._interface) : null;
                if (model_implementation == null || !model_implementation._should_serialize || model_implementation._parameters == null) continue;

                java_writer.beginControlFlow(String.format("%s (_scope.equals(\"%s\"))", nested_if_else_if, entry.getKey()));
                for (Element parameter : model_implementation._parameters) {
                    final ModelData sub_model = element_to_model_interfaces.get(_type_utilities.asElement(parameter.asType()));
                    // This should never actually occur without error in Annotation Processor
                    if (sub_model == null) {
                        processingEnv.getMessager().printMessage(ERROR, "Unexpected error, Model is null");
                        continue;
                    }
                    if (!sub_model._should_serialize) continue;
                    java_writer.emitStatement("store(createModel(%s.class))", sub_model._interface);
                }
                java_writer.endControlFlow();
                nested_if_else_if = else_if;
            }
            if (nested_if_else_if.equals(else_if)) {
                ModelData model_implementation = model_implementations.get(PRODUCTION).get(model._interface);
                if (model_implementation == null || !model_implementation._should_serialize || model_implementation._parameters == null) continue;
                java_writer.beginControlFlow("else");
                for (Element parameter : model_implementation._parameters) {
                    final ModelData sub_model = element_to_model_interfaces.get(_type_utilities.asElement(parameter.asType()));
                    // This should never actually occur without error in Annotation Processor
                    if (sub_model == null) {
                        processingEnv.getMessager().printMessage(ERROR, "Unexpected error, Model is null");
                        continue;
                    }
                    if (!sub_model._should_serialize) continue;
                    java_writer.emitStatement("store(createModel(%s.class))", sub_model._interface);
                }
                java_writer.endControlFlow();
            }
            java_writer.endControlFlow();
            if_else_if_control = else_if;
        }
        java_writer.endMethod();
        // createObjectGraph
        if_else_if_control = "if";
        java_writer.emitEmptyLine()
                   .emitAnnotation(Override.class)
                   .emitAnnotation(Nonnull.class)
                   .beginMethod(JavaWriter.type(ObjectGraph.class), "createObjectGraph", protected_modifier)
                   .emitStatement("final List<Object> modules = new ArrayList<Object>()")
                   .emitSingleLineComment("Controller modules");
        if (!controller_modules.isEmpty()) {
            String production_module = null;
            for (ModuleData controller_module : controller_modules)
                if (controller_module._scope.equals(Implementations.PRODUCTION)) {
                    production_module = String.format(Locale.US, "modules.add(new %s())", controller_module._qualified_name);
                } else {
                    java_writer.beginControlFlow(String.format(Locale.US, "%s (_scope.equals(\"%s\"))"
                            , if_else_if_control, controller_module._scope))
                            .emitStatement("modules.add(new %s())", controller_module._qualified_name)
                            .endControlFlow();
                    if_else_if_control = else_if;
                }
            if (production_module != null) java_writer.emitStatement(production_module);
        }
        java_writer.emitSingleLineComment("Model modules");
        if (!model_modules.isEmpty()) {
            if_else_if_control = "if";
            String production_module = null;
            for (ModuleData model_module : model_modules)
                if (model_module._scope.equals(Implementations.PRODUCTION)) {
                    production_module = String.format(Locale.US, "modules.add(new %s(_log, this))", model_module._qualified_name);
                } else {
                    java_writer.beginControlFlow(String.format(Locale.US, "%s (_scope.equals(\"%s\"))"
                            , if_else_if_control, model_module._scope))
                            .emitStatement("modules.add(new %s(_log, this))", model_module._qualified_name)
                            .endControlFlow();
                    if_else_if_control = else_if;
                }
            if (production_module != null) java_writer.emitStatement(production_module);
        }
        java_writer.emitStatement("return ObjectGraph.create(modules.toArray())")
                   .endMethod();
        // bindPresentationsToControllers
        java_writer.emitEmptyLine()
                   .emitAnnotation(Override.class)
                   .emitAnnotation(Nonnull.class)
                   .beginMethod(JavaWriter.type(ImmutableMap.class, JavaWriter.type(Class.class, "?"), JavaWriter.type(Provider.class))
                           , "bindPresentationsToControllers", protected_modifier)
                   .emitStatement("return ImmutableMap.<Class<?>, Provider>builder()%n%s.build()", controller_puts)
                   .endMethod();
        // provideModelImplementations
        java_writer.emitEmptyLine()
                   .emitAnnotation(Override.class)
                   .emitAnnotation(Nonnull.class)
                   .beginMethod(JavaWriter.type(ImmutableMap.class, JavaWriter.type(Class.class, "?"), JavaWriter.type(Lazy.class))
                           , "provideModelImplementations", protected_modifier)
                   .emitStatement("return ImmutableMap.<Class<?>, Lazy>builder()%n%s.build()", model_puts)
                   .endMethod();
	    java_writer.endType()
				   .emitEmptyLine();
		java_writer.close();
	}
	
	/**
     * TODO: move Controller Bus provider to a Segue Controller module?
	 * @param writer
	 * @param package_name
	 * @param controllers
	 * @param class_name
	 */
	private void generateControllerModule(Writer writer, String package_name, List<ControllerData> controllers, 
			                             String class_name) throws IOException {
		final EnumSet<Modifier> public_modifier = EnumSet.of(PUBLIC);
		JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
		java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
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
			controller_list.append("<li>{@link ").append(controller._interface).append("}</li>\n");
		}
		java_writer.emitJavadoc("<p>Module for injecting:\n" +
				                "<ul>\n" +
				                "%s" +
				                "</ul></p>", controller_list)
				   .emitAnnotation(Module.class, ImmutableMap.of(
						   "injects",
						   "{\n" +
								   "_SegueController.class" +
						   "\n}",
						   "overrides", !class_name.equals(_DEFAULT_CONTROLLER_MODULE),
						   "library", true,
						   "complete", false))
					.beginType(class_name, "class", public_modifier)
					.emitEmptyLine()
					.emitAnnotation(Provides.class)
					.emitAnnotation(Singleton.class)
					.emitAnnotation(Named.class, ControllerContract.BUS)
					.beginMethod("com.squareup.otto.Bus", "providesControllerBus", EnumSet.noneOf(Modifier.class))
					.emitStatement("return new Bus(\"Controller Bus\")")
					.endMethod();
		// Controller providers
		for (ControllerData controller : controllers)
			java_writer.emitEmptyLine()
			           .emitAnnotation(Provides.class)
			           .beginMethod(controller._interface + "", "provides" + controller._interface.getSimpleName(),
			        		        EnumSet.noneOf(Modifier.class))
			           .emitStatement("return new %s()", controller._implementation)
			           .endMethod();
		java_writer.endType();
		java_writer.close();
	}
	
	/**
	 * @param writer
	 * @param package_name
	 * @param variable_name
	 * @param class_name
	 */
	private void generateDataSourceInjector(Writer writer, String package_name, Element target, String variable_name,
			                                String class_name) throws IOException {
		final JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
		java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
		           .emitPackage(package_name)
		           .emitImports(JavaWriter.type(Finder.class))
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Data Source into {@link %s}'s %s.</p>", target, variable_name)
			       .beginType(class_name, "class", EnumSet.of(PUBLIC, FINAL))
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Data Source into {@link %s}'s %s.</p>\n" +
                                "@param segue_controller The Segue Controller\n" +
			       		        "@param finder The finder that specifies how to retrieve the context from the target\n" +
			       		        "@param target The target of the injection", target, variable_name)
			       .beginMethod("void", "injectDataSource", EnumSet.of(PUBLIC, STATIC),
                                JavaWriter.type(SegueController.class), "segue_controller",
			    		        JavaWriter.type(Finder.class), "finder", 
			    		        target + "", "target")
			       .emitStatement("target.%s = " +
			       		"segue_controller.dataSource(" +
			       		"finder.findContext(target).getClass())", 
			       		variable_name)
			       .endMethod()
			       .endType()
			       .emitEmptyLine();
		java_writer.close();
	}
	
	/**
     * TODO: should there be a no-op GsonConverter for when model is serialized only under certain scopes? Or move that declaration to @Model
	 * @param writer
	 * @param package_name
	 * @param models
	 * @param class_name
	 */
	private void generateModelModule(Writer writer, String package_name, List<ModelData> models, String class_name) 
			throws IOException {
        final String file_tested = "_file_tested";
        final EnumSet<Modifier> private_final = EnumSet.of(PRIVATE, FINAL);
        final EnumSet<Modifier> private_modifier = EnumSet.of(PRIVATE);
		final JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
		java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
				   .emitPackage(package_name)
				   .emitImports("com.imminentmeals.prestige._SegueController",
						        "dagger.Module",
						        "dagger.Provides",
						        "javax.inject.Singleton",
                                JavaWriter.type(Gson.class),
                                JavaWriter.type(GsonProvider.class),
                                JavaWriter.type(GsonConverter.class),
                                JavaWriter.type(InputStream.class),
                                JavaWriter.type(IOException.class),
                                JavaWriter.type(Named.class),
                                JavaWriter.type(ExclusionStrategy.class),
                                JavaWriter.type(FieldAttributes.class),
                                JavaWriter.type(InstanceCreator.class),
                                JavaWriter.type(Set.class),
                                JavaWriter.type(SegueController.class))
                   .emitStaticImports(JavaWriter.type(Sets.class) + ".newHashSet")
				   .emitEmptyLine();
		final StringBuilder model_list = new StringBuilder();
		for (ModelData model : models) {
			model_list.append("<li>{@link ").append(model._interface).append("}</li>\n");
		}
		java_writer.emitJavadoc("<p>Module for injecting:\n" +
                                "<ul>\n" +
                                "%s" +
                                "</ul></p>", model_list)
				   .emitAnnotation(Module.class, ImmutableMap.of(
                           "injects",
                           "{\n" +
                                   "_SegueController.class" +
                                   "\n}",
                           "overrides", !class_name.equals(_DEFAULT_MODEL_MODULE),
                           "library", true,
                           "complete", false))
				   .beginType(class_name, "class", EnumSet.of(PUBLIC))
				   .emitEmptyLine()
                   .beginMethod(null, class_name, EnumSet.of(PUBLIC), JavaWriter.type(Timber.class), "log"
                              , JavaWriter.type(SegueController.class), "segue_controller")
                   .emitStatement("_log = log")
                   .emitStatement("_segue_controller = segue_controller")
                   .endMethod()
                   .emitEmptyLine()
                   .emitAnnotation(Provides.class)
                   .emitAnnotation(Singleton.class)
                   .beginMethod(JavaWriter.type(Gson.class), "providesGson", EnumSet.noneOf(Modifier.class)
                              , JavaWriter.type(GsonProvider.class), "gson_provider")
                   .emitStatement("return buildGson(gson_provider.gsonBuilder())")
                   .endMethod();
		// Model providers
		for (ModelData model : models) {
            java_writer.emitEmptyLine()
                       .emitAnnotation(Provides.class)
                       .emitAnnotation(Singleton.class);
            String[] provider_parameters = model._should_serialize
                    ? new String[] {
                        JavaWriter.type(GsonProvider.class)
                      , "gson_provider"
                      , "@Named(" + stringLiteral(model._interface + "")+ ") " + JavaWriter.type(GsonConverter.class)
                      , "converter" }
                    : new String[0];
            final String[] new_instance_format_parameters;
			if (model._parameters == null || model._parameters.isEmpty()) {
                new_instance_format_parameters = new String[] { java_writer.compressType(model._implementation + ""), "" };
            } else {
				final Set<String> provider_method_parameters = newLinkedHashSet(Arrays.asList(provider_parameters));
				final List<String> constructor_parameters = newArrayList();
				for (VariableElement parameter : model._parameters) {
                    final String type = _type_utilities.asElement(parameter.asType()) + "";
                    if (!provider_method_parameters.contains(type)) {
                        provider_method_parameters.add(type);
                        provider_method_parameters.add(parameter + "");
                    }
					constructor_parameters.add(parameter + "");
				}
                provider_parameters = provider_method_parameters.toArray(provider_parameters);
                new_instance_format_parameters = new String[] {
                        java_writer.compressType(model._implementation + "")
                      , Joiner.on(", ").join(constructor_parameters) };
			}
            java_writer.beginMethod(model._interface + "", "provides" + model._interface.getSimpleName(),
                    EnumSet.noneOf(Modifier.class), provider_parameters);
            if (model._should_serialize) {
                java_writer.emitStatement("InputStream input_stream = null")
                           .beginControlFlow("try")
                           .beginControlFlow(String.format("if (!_%s%s)", model._variable_name, file_tested))
                           .emitStatement("_%s%s = true", model._variable_name, file_tested)
                           .emitStatement("input_stream = gson_provider.inputStreamFor(%s.class)", model._interface)
                           .emitStatement("_log.tag(_TAG).d(\"Restoring %s from input stream\")", java_writer.compressType(model._implementation + ""))
                           .emitStatement("return (%s) converter.from(input_stream)", java_writer.compressType(model._implementation + ""))
                           .nextControlFlow("else")
                           .emitStatement("return new %s(%s)", new_instance_format_parameters)
                           .endControlFlow()
                           .nextControlFlow("catch (Exception _)")
                           .emitStatement("_log.tag(_TAG).d(\"Nothing to restore; creating model %s\")", model._interface)
                           .emitStatement("return new %s(%s)", new_instance_format_parameters)
                           .nextControlFlow("finally")
                           .beginControlFlow("if (input_stream != null)")
                           .beginControlFlow("try")
                           .emitStatement("input_stream.close()")
                           .nextControlFlow("catch (IOException _)")
                           .endControlFlow()
                           .endControlFlow()
                           .endControlFlow()
                           .endMethod();

                // Creates the GsonConverter
                java_writer.emitEmptyLine()
                        .emitAnnotation(Provides.class)
                        .emitAnnotation(Singleton.class)
                        .emitAnnotation(Named.class, stringLiteral(model._interface + ""))
                        .beginMethod(JavaWriter.type(GsonConverter.class),
                                "provides" + model._implementation.getSimpleName() + "Converter",
                                EnumSet.noneOf(Modifier.class), JavaWriter.type(Gson.class), "gson")
                        .emitStatement("return new %s(gson, %s.class)"
                                , JavaWriter.type(GsonConverter.class, model._implementation + ""), model._implementation)
                        .endMethod();
            } else {
                java_writer.emitStatement("return new %s(%s)", new_instance_format_parameters)
                           .endMethod();
            }
        }
		java_writer.emitEmptyLine()
                   .beginMethod(JavaWriter.type(Gson.class), "buildGson", EnumSet.of(PRIVATE)
                              , JavaWriter.type(GsonBuilder.class), "gson_builder");
        final Joiner new_line_joiner = Joiner.on("%n");
        final List<String> model_exclusions = newArrayListWithCapacity(models.size());
        final String model_exclusion = "(Class) %s.class";
        for (ModelData model : models) {
            model_exclusions.add(String.format(model_exclusion, model._interface));
            if (model._should_serialize) {
                java_writer.emitStatement("final InstanceCreator<%1$s> %1$s_creator = _segue_controller.instanceCreator(%2$s.class)"
                                        , java_writer.compressType(model._implementation + ""), model._interface)
                           .emitStatement("gson_builder.registerTypeAdapter(%1$s.class, %1$s_creator)"
                                   , java_writer.compressType(model._implementation + ""));
            }
        }
        java_writer.emitStatement(new_line_joiner.join(
                "gson_builder.addSerializationExclusionStrategy(new ExclusionStrategy() {"
              , "    public boolean shouldSkipField(FieldAttributes field) {"
              , "      return _models.contains(field.getDeclaredClass());"
              , "    }"
              , "    public boolean shouldSkipClass(Class<?> _) {"
              , "      return false;"
              , "    }"
              , "    private final Set<Class> _models = newHashSet(%s);"
              , "})"), Joiner.on(",\n\t\t").join(model_exclusions))
                   .emitStatement("return gson_builder.create()")
                   .endMethod()
                   .emitEmptyLine()
                   .emitJavadoc("Log where messages are written")
                   .emitField(JavaWriter.type(Timber.class), "_log", private_final)
                   .emitField(JavaWriter.type(String.class), "_TAG", EnumSet.of(PRIVATE, STATIC, FINAL), stringLiteral("Prestige"))
                   .emitJavadoc("Segue Controller")
                   .emitField(JavaWriter.type(SegueController.class), "_segue_controller", private_final);
        for (ModelData model : models)
            if (model._should_serialize)
                java_writer.emitField(JavaWriter.type(boolean.class), '_' + model._variable_name + file_tested, private_modifier, "false");
        java_writer.endType();
		java_writer.close();
	}
	
	/**
	 * @param writer
	 * @param package_name
	 * @param class_name
	 */
	private void generateModelInjector(Writer writer, String package_name, List<ModelInjectionData> injections,
			                           String class_name, Element target) throws IOException {
		final JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
		java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
		           .emitPackage(package_name)
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Models into {@link %s} and stores them for later use.</p>", class_name)
			       .beginType(class_name, "class", EnumSet.of(PUBLIC, FINAL))
			       .emitEmptyLine()
			       .emitJavadoc("<p>Injects the Models into {@link %s}.</p>\n" +
			       		        "@param segue_controller The Segue Controller from which to retrieve Models\n" +
			       		        "@param target The target of the injection", target)
			       .beginMethod("void", "injectModels", EnumSet.of(PUBLIC, STATIC),
			    		        JavaWriter.type(SegueController.class), "segue_controller",
                                ((TypeElement) target).getQualifiedName() + "", "target");
		for (ModelInjectionData injection : injections)
			java_writer.emitStatement("target.%s = segue_controller.createModel(%s.class)", injection._variable_name,
					                  injection._variable.asType());
		java_writer.endMethod()
                   .emitEmptyLine()
                   .emitJavadoc("<p>Stores the Models from {@link %s}.</p>\n" +
                           "@param segue_controller The Segue Controller used to store Models\n" +
                           "@param source The source of models to store", target)
                   .beginMethod("void", "storeModels", EnumSet.of(PUBLIC, STATIC),
                           newArrayList(JavaWriter.type(SegueController.class), "segue_controller",
                           ((TypeElement) target).getQualifiedName() + "", "source"),
                           newArrayList(JavaWriter.type(IOException.class)));
        for (ModelInjectionData injection : injections)
            java_writer.emitStatement("segue_controller.store(source.%s)", injection._variable_name);
        java_writer.endMethod()
			       .endType()
			       .emitEmptyLine();
		java_writer.close();
	}

	private void generateControllerPresentationFragmentInjector(Writer writer, String package_name, 
			List<PresentationFragmentInjectionData> injections, String class_name, Element target) throws IOException {
		final JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
		java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
		           .emitPackage(package_name)
		           .emitEmptyLine()
		           .emitEmptyLine()
		           .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>", class_name)
		           .beginType(class_name, "class", EnumSet.of(PUBLIC, FINAL))
		           .emitEmptyLine()
		           .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>\n" +
                           "@param target The target of the injection\n" +
                           "@param presentation_fragment The presentation fragment to inject\n" +
                           "@param tag The tag that labels the presentation fragment", target)
		           .beginMethod("void", "attachPresentationFragment",
                           EnumSet.of(PUBLIC, STATIC),
                           processingEnv.getElementUtils().getBinaryName((TypeElement) target) + "", "target",
                           JavaWriter.type(Object.class), "presentation_fragment",
                           JavaWriter.type(String.class), "tag");
		final String control_format = "%s (presentation_fragment instanceof %s &&%n" +
		        		              "\ttag.equals(\"%s\"))";
		if (!injections.isEmpty()) {
			final PresentationFragmentInjectionData injection = injections.get(0);
			java_writer.beginControlFlow(String.format(control_format, "if", injection._implementation, injection._tag))
			           .emitStatement("target.%s = (%s) presentation_fragment", injection._variable_name, 
			        		          injection._variable.asType())
			           .endControlFlow();
		}
		for (PresentationFragmentInjectionData injection : injections.subList(min(1, injections.size()), injections.size()))
			java_writer.beginControlFlow(String.format(control_format, "else if", injection._implementation, injection._tag))
		               .emitStatement("target.%s = (%s) presentation_fragment", injection._variable_name,
		            		          injection._variable.asType())
		               .endControlFlow();
		
		java_writer.endMethod()
		           .endType()
		           .emitEmptyLine();
		java_writer.close();
	}
	
	@SuppressLint("NewApi")
    private void generatePresentationFragmentInjector(Writer writer, String package_name,
			Map<Integer, List<PresentationFragmentInjectionData>> injections, String class_name, Element target) throws IOException {
		final JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
		java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
		           .emitPackage(package_name)
		           .emitEmptyLine()
		           .emitImports(JavaWriter.type(FragmentManager.class),
		        		        JavaWriter.type(Fragment.class),
		        		        JavaWriter.type(Finder.class),
		        		        JavaWriter.type(Context.class))
		           .emitEmptyLine()
		           .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>", target)
		           .beginType(class_name, "class", EnumSet.of(PUBLIC, FINAL))
		           .emitEmptyLine()
		           .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>\n" +
		                        "@param finder The finder that specifies how to retrieve the context\n" +
		        		        "@param display The current display state\n" +
		        		        "@param target The target of the injection", target)
		           .beginMethod("void", "injectPresentationFragments", 
		        		        EnumSet.of(PUBLIC, STATIC),
		        		        JavaWriter.type(Finder.class), "finder",
		        		        JavaWriter.type(int.class), "display",
		        		        processingEnv.getElementUtils().getBinaryName((TypeElement) target) + "", "target");
		if (!injections.isEmpty()) {
			java_writer.emitStatement("final Context context = finder.findContext(target)")
			           .emitStatement("final FragmentManager fragment_manager = finder.findFragmentManager(target)")
			           .beginControlFlow("switch (display)");
			for (Entry<Integer, List<PresentationFragmentInjectionData>> entry : injections.entrySet()) {
				java_writer.beginControlFlow("case " + entry.getKey() + ":");
				final StringBuilder transactions = new StringBuilder();
				for (PresentationFragmentInjectionData injection : entry.getValue()) {
					java_writer.emitStatement("target.%s = (%s) Fragment.instantiate(context, \"%s\")", injection._variable_name,
			                                  injection._variable.asType(), injection._implementation);
	                transactions.append("\t.add(")
                                .append(injection._displays.get(entry.getKey()))
                                .append(",\n")
                                .append("\t(Fragment) \ttarget.")
                                .append(injection._variable_name);
	                if (!injection._tag.isEmpty())
	                	transactions.append(",\n\"").append(injection._tag).append("\"");
	                transactions.append(")\n");
				}
				java_writer.emitStatement("fragment_manager.beginTransaction()\n" + transactions + "\t.commit()")
						   .emitStatement("break")
		                   .endControlFlow();
			}
			java_writer.endControlFlow();
		}
		java_writer.endMethod()
                   .endType()
                   .emitEmptyLine();
		java_writer.close();
	}

    private void generateControllerPresentationInjector(Writer writer, String package_name, PresentationInjectionData injection,
                                                        String class_name, Element target) throws IOException {
        final JavaWriter java_writer = new JavaWriter(writer);
        java_writer.setCompressingTypes(true);
        java_writer.emitSingleLineComment("Generated code from Prestige. Do not modify!")
                .emitPackage(package_name)
                .emitEmptyLine()
                .emitEmptyLine()
                .emitJavadoc("<p>Injects the Presentation into {@link %s}.</p>", class_name)
                .beginType(class_name, "class", EnumSet.of(PUBLIC, FINAL))
                .emitEmptyLine()
                .emitJavadoc("<p>Injects the Presentation into {@link %s}.</p>\n" +
                        "@param target The target of the injection\n" +
                        "@param presentation The presentation to inject", target)
                .beginMethod("void", "attachPresentation",
                        EnumSet.of(PUBLIC, STATIC),
                        processingEnv.getElementUtils().getBinaryName((TypeElement) target) + "", "target",
                        JavaWriter.type(Object.class), "presentation");
        final String control_format = "if (presentation instanceof %s)";
        java_writer.beginControlFlow(String.format(Locale.US, control_format, injection._variable.asType()))
                .emitStatement("target.%s = (%s) presentation", injection._variable_name, injection._variable.asType())
                .endControlFlow();

        java_writer.endMethod()
                .endType()
                .emitEmptyLine();
        java_writer.close();
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
				found_accessible_constructor = !member.getModifiers().contains(PRIVATE);
				break;
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
		processingEnv.getMessager().printMessage(ERROR,
				String.format(message, arguments), element);
	}

    /**
     * <p>Produces an note message with the given information.</p>
     * @param element The element to relate the note message to
     * @param message The message to send
     * @param arguments Arguments to format into the message
     */
    private void note(Element element, String message, Object... arguments) {
        processingEnv.getMessager().printMessage(NOTE,
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
		private final List<? extends VariableElement> _parameters;
		private final String _variable_name;
        private final boolean _should_serialize;

        /**
         * <p>
         * Constructs a {@link ModelData}.
         * <p>
         *
         * @param model The @Model
         * @param should_serialize Indicates if serialization logic should be implemented for the model
         */
        public ModelData(@Nonnull Element model, boolean should_serialize) {
            this(model, null, null, should_serialize);
        }

		/**
		 * <p>
		 * Constructs a {@link ModelData}.
		 * <p>
		 * 
		 * @param model The @Model
		 * @param model_implementation The implementation of the @Model
         * @param parameters Parameters of the model implementation's constructor (list of models on which it depends)
         * @param should_serialize Indicates if serialization logic should be implemented for the model
		 */
		public ModelData(@Nonnull Element model, Element model_implementation, List<? extends VariableElement> parameters
                       , boolean should_serialize) {
			_implementation = model_implementation;
			_interface = model;
			_parameters = parameters;
			_variable_name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, model.getSimpleName() + "");
            _should_serialize = should_serialize;
		}

        @Override
        public int hashCode() {
            return _interface.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ModelData)) return false;
            final ModelData other = (ModelData) object;
            return _interface.equals(other._interface)
                && (_implementation == null || other._implementation == null
                 || _implementation.equals(other._implementation));
        }

        @Override
        public String toString() {
            return _interface + "";
        }
    }
	
	/**
	 * <p>Container for Presentation Fragment data.</p>
	 * @author Dandre Allison
	 */
	private static class PresentationFragmentData extends PresentationData {
		
		/**
		 * <p>Constructs a {@link PresentationData}.</p>
		 * @param protocol The Protocol
		 * @param implementation The presentation implementation
		 */
		public PresentationFragmentData(Element protocol, Element implementation) {
			super(protocol, implementation);
		}

		@Override
		public String toString() {
			return String.format(format, protocol, implementation);
		}
		
		private static final String format = "{protocol: %s, implementation: %s}";
	}
	
	/**
	 * <p>Container for Presentation data.</p>
	 * @author Dandre Allison
	 */
	private static class PresentationData {
		/** The Protocol */
		protected final Element protocol;
		/** The Presentation implementation */
		protected final Element implementation;
		
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
		private final Element _variable;
		private final String _variable_name;
		private final String _class_name;
        private final boolean _should_serialize;
		
		/**
		 * <p>Constructs a {@link ModelInjectionData}.</p>
		 * @param variable The variable in which to inject the Model
		 */
		public ModelInjectionData(String package_name, Element variable, String element_class, boolean should_serialize) {
			_package_name = package_name;
			_variable = variable;
			_variable_name = variable.getSimpleName() + "";
			_class_name = element_class.substring(package_name.length() + 1) + MODEL_INJECTOR_SUFFIX;
            _should_serialize = should_serialize;
		}
	}
	
	private static class PresentationFragmentInjectionData {
		private final String _package_name;
		private final Element _variable;
		private final String _variable_name;
		private final String _class_name;
		private final Map<Integer, Integer> _displays;
		private final String _tag;
		private final Element _implementation;
		private final boolean _is_manually_created;
		
		public PresentationFragmentInjectionData(String package_name, Element variable, String element_class,
				int[] displays, String tag, Element implementation, boolean is_manually_created) {
			assert displays.length % 2 == 0;
			
			_package_name = package_name;
			_variable = variable;
			_variable_name = variable.getSimpleName() + "";
			_class_name = element_class.substring(package_name.length() + 1) + PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;
			_implementation = implementation;
			_is_manually_created = is_manually_created;
			
			if (_is_manually_created) {
				_displays = null;
				_tag = null;
				return;
			}
			
			final ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();
			for (int i = 0; i < displays.length; i += 2)
				builder.put(displays[i], displays[i + 1]);
			_displays = builder.build();
			_tag = tag;
		}
	}

    private static class PresentationInjectionData {
        private final String _package_name;
        private final Element _variable;
        private final String _variable_name;
        private final String _class_name;

        public PresentationInjectionData(String package_name, Element variable, String element_class) {
            _package_name = package_name;
            _variable = variable;
            _variable_name = variable.getSimpleName() + "";
            _class_name = element_class.substring(package_name.length() + 1) + PRESENTATION_INJECTOR_SUFFIX;
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
    private static final String _CONVERTOR = "_converter";
	/** Counts the number of passes The Annotation Processor has performed */
	private int _passes = 0;
	private Elements _element_utilities;
	private Types _type_utilities;
}