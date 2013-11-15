package com.imminentmeals.prestige.codegen;

import android.annotation.SuppressLint;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
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
import com.imminentmeals.prestige.annotations.meta.Implementations;
import com.squareup.javawriter.JavaWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import dagger.Lazy;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import timber.log.Timber;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.CONTROLLER_MODULE_SUFFIX;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.DATA_SOURCE_INJECTOR_SUFFIX;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.MODEL_INJECTOR_SUFFIX;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.MODEL_MODULE_SUFFIX;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.PRESENTATION_INJECTOR_SUFFIX;
import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static java.lang.Math.min;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/* package */ class CodeGenerator {

  /* package */CodeGenerator(Filer filer, Messager messager, Elements elements, Types types) {
    _filer = filer;
    _messager = messager;
    _elements = elements;
    _types = types;
  }

  @SuppressWarnings("unchecked")
  public void generateSourceCode(List<PresentationControllerBinding> controllers, List<ModuleData> controller_modules,
      List<DataSourceInjectionData> data_source_injections,
      List<ModuleData> model_modules, List<ModelData> model_interfaces,
      Map<Element, ModelData> element_to_model_interface,
      Map<String, Map<Element, ModelData>> model_implementations,
      Map<Element, List<ModelInjectionData>> model_injections,
      Map<Element, Map<Integer, List<PresentationFragmentInjectionData>>> presentation_fragment_injections,
      Map<Element, List<PresentationFragmentInjectionData>> controller_presentation_fragment_injections,
      Map<Element, PresentationInjectionData> controller_presentation_injections) {
    Writer writer = null;
    try {
      // Generates the _SegueController
      JavaFileObject source_code = _filer.createSourceFile(_SEGUE_CONTROLLER_CLASS, (Element) null);
      writer = source_code.openWriter();
      writer.flush();
      generateSegueControllerSourceCode(writer, controllers, controller_modules, model_modules, model_interfaces
          , element_to_model_interface, model_implementations);

      // Generates the *ControllerModules
      for (ModuleData controller_module : controller_modules) {
        source_code = _filer.createSourceFile(controller_module.qualified_name, (Element) null);
        writer = source_code.openWriter();
        writer.flush();
        generateControllerModule(writer, controller_module.package_name,
            (List<ControllerData>) controller_module.components, controller_module.class_name);
      }

      // Generates the $$DataSourceInjectors
      for (DataSourceInjectionData data_source_injection : data_source_injections) {
        final TypeElement element = (TypeElement) data_source_injection.target;
        source_code = _filer.createSourceFile(_elements.getBinaryName(element) + DATA_SOURCE_INJECTOR_SUFFIX,
            element);
        writer = source_code.openWriter();
        writer.flush();
        generateDataSourceInjector(writer, data_source_injection.package_name, data_source_injection.target,
            data_source_injection.variable_name, data_source_injection.class_name);
      }

      // Generates the *ModelModules
      final boolean has_default_models = model_implementations.containsKey(PRODUCTION);
      String default_model_module = null;
      if (has_default_models)
        for (ModuleData model_module : model_modules)
          if (model_module.class_name.equals(_DEFAULT_MODEL_MODULE)) {
            default_model_module = model_module.qualified_name;
            break;
          }
      for (ModuleData model_module : model_modules) {
        source_code = _filer.createSourceFile(model_module.qualified_name, (Element) null);
        writer = source_code.openWriter();
        writer.flush();
        generateModelModule(writer, model_module.package_name, (List<ModelData>) model_module.components,
            model_module.class_name, has_default_models, default_model_module);
      }

      // Generates the $$ModelInjectors
      for (Map.Entry<Element, List<ModelInjectionData>> injection : model_injections.entrySet()) {
        final TypeElement element = (TypeElement) injection.getKey();
        final String full_name = _elements.getBinaryName(element) + MODEL_INJECTOR_SUFFIX;
        source_code = _filer.createSourceFile(full_name, element);
        writer = source_code.openWriter();
        writer.flush();
        final String package_name = _elements.getPackageOf(element) + "";
        final String class_name = full_name.substring(package_name.length() + 1);
        generateModelInjector(writer, package_name, injection.getValue(), class_name, element);
      }

      // Generates the $$PresentationFragmentInjectors
      for (Map.Entry<Element, Map<Integer, List<PresentationFragmentInjectionData>>> injection : presentation_fragment_injections.entrySet()) {
        final TypeElement element = (TypeElement) injection.getKey();
        final String full_name = _elements.getBinaryName(element) + PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;
        source_code = _filer.createSourceFile(full_name, element);
        writer = source_code.openWriter();
        writer.flush();
        final String package_name = _elements.getPackageOf(element) + "";
        final String class_name = full_name.substring(package_name.length() + 1);
        generatePresentationFragmentInjector(writer, package_name, injection.getValue(), class_name, element);
      }
      for (Map.Entry<Element, List<PresentationFragmentInjectionData>> injection : controller_presentation_fragment_injections.entrySet()) {
        final TypeElement element = (TypeElement) injection.getKey();
        final String full_name = _elements.getBinaryName(element) + PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;
        source_code = _filer.createSourceFile(full_name, element);
        writer = source_code.openWriter();
        writer.flush();
        final String package_name = _elements.getPackageOf(element) + "";
        final String class_name = full_name.substring(package_name.length() + 1);
        generateControllerPresentationFragmentInjector(writer, package_name, injection.getValue(), class_name, element);
      }

      // Generates the $$PresentationInjectors
      for (Map.Entry<Element, PresentationInjectionData> injection : controller_presentation_injections.entrySet()) {
        final TypeElement element = (TypeElement) injection.getKey();
        final String full_name = _elements.getBinaryName(element) + PRESENTATION_INJECTOR_SUFFIX;
        source_code = _filer.createSourceFile(full_name, element);
        writer = source_code.openWriter();
        writer.flush();
        final String package_name = _elements.getPackageOf(element) + "";
        final String class_name = full_name.substring(package_name.length() + 1);
        generateControllerPresentationInjector(writer, package_name, injection.getValue(), class_name, element);
      }
    } catch (IOException exception) {
      _messager.printMessage(ERROR, exception.getMessage());
    } finally {
      try {
        Closeables.close(writer, writer != null);
      } catch (IOException exception) {
        _messager.printMessage(ERROR, exception.getMessage());
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
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage("com.imminentmeals.prestige")
        .emitImports(JavaWriter.type(ImmutableMap.class)
            , JavaWriter.type(GsonConverter.class)
            , JavaWriter.type(IOException.class)
            , "java.util.ArrayList"
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
        .beginType(_SEGUE_CONTROLLER_CLASS, _CLASS, public_modifier
            // extends
            , JavaWriter.type(SegueController.class));
    final StringBuilder controller_puts = new StringBuilder();
    for (PresentationControllerBinding binding : controllers) {
      java_writer.emitJavadoc("Provider for instances of the {@link %s} Controller", binding.controller)
          .emitAnnotation(Inject.class)
          .emitField("javax.inject.Provider<" + binding.controller + ">", binding.variable_name);
      controller_puts.append(String.format(".put(%s.class, %s)%n",
          binding.presentation_implementation, binding.variable_name));
    }

    final StringBuilder model_puts = new StringBuilder();
    emitModelFields(java_writer, models, model_puts);

    // Constructor
    java_writer.emitEmptyLine()
        .emitJavadoc("<p>Constructs a {@link SegueController}.</p>")
        .beginMethod(null, _SEGUE_CONTROLLER_CLASS, public_modifier,
            JavaWriter.type(String.class), "scope",
            JavaWriter.type(Timber.class), "log")
        .emitStatement("super(scope, log)")
        .endMethod();

    // SegueController Contract
    emitStoreMethod(java_writer, models, element_to_model_interfaces, model_implementations, public_modifier, else_if);
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
        if (controller_module.scope.equals(Implementations.PRODUCTION)) {
          production_module = String.format(Locale.US, "modules.add(new %s())", controller_module.qualified_name);
        } else {
          java_writer.beginControlFlow(String.format(Locale.US, "%s (scope.equals(\"%s\"))"
              , if_else_if_control, controller_module.scope))
              .emitStatement("modules.add(new %s())", controller_module.qualified_name)
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
        if (model_module.scope.equals(Implementations.PRODUCTION)) {
          production_module = String.format(Locale.US, "modules.add(new %s(log, this))", model_module.qualified_name);
        } else {
          java_writer.beginControlFlow(String.format(Locale.US, "%s (scope.equals(\"%s\"))"
              , if_else_if_control, model_module.scope))
              .emitStatement("modules.add(new %s(log, this))", model_module.qualified_name)
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

  private static StringBuilder emitModelFields(JavaWriter java_writer, List<ModelData> models, StringBuilder model_puts)
      throws IOException {
    for (ModelData model : models) {
      java_writer.emitJavadoc("Provider for instances of the {@link %s} Model", model.contract)
          .emitAnnotation(Inject.class)
          .emitField("dagger.Lazy<" + model.contract + ">", model.variable_name);
      model_puts.append(String.format(".put(%s.class, %s)%n",
          model.contract, model.variable_name));
      if (model.should_serialize)
        java_writer.emitJavadoc("Provider for {@link %s} for {@link %s}", JavaWriter.type(GsonConverter.class)
            , model.contract)
            .emitAnnotation(Inject.class)
            .emitAnnotation(Named.class, stringLiteral(model.contract + ""))
            .emitField(JavaWriter.type(Lazy.class, JavaWriter.type(GsonConverter.class))
                , model.variable_name + _CONVERTOR);
    }
    return model_puts;
  }

  private void emitStoreMethod(JavaWriter java_writer, List<ModelData> models, Map<Element, ModelData> element_to_model_interfaces
      , Map<String, Map<Element, ModelData>> model_implementations, EnumSet<Modifier> public_modifier
      , String else_if) throws IOException {
    String if_else_if_control;
    java_writer.emitEmptyLine()
        .emitAnnotation(Override.class)
        .beginMethod("<T> void", "store", public_modifier, newArrayList("T", "object")
            , newArrayList(JavaWriter.type(IOException.class)));
    if_else_if_control = "if";
    String nested_if_else_if;
    for (ModelData model : models) {
      if (!model.should_serialize) continue;

      nested_if_else_if = "if";
      java_writer.beginControlFlow(String.format("%s (object instanceof %s)", if_else_if_control, model.contract))
          .emitStatement("log.tag(_TAG).d(\"Storing \" + object)")
          .emitStatement("%s%s.get().toStream(object, gson_provider.get().outputStreamFor(%s.class))"
              , model.variable_name, _CONVERTOR, model.contract);

      // TODO: avoid storing the same model multiple times in one pass
      for (Map.Entry<String, Map<Element, ModelData>> entry : model_implementations.entrySet()) {
        final ModelData model_implementation = modelImplementation(model, model_implementations, entry.getValue());
        if (shouldSkipModelImplementation(model_implementation)) continue;

        java_writer.beginControlFlow(String.format("%s (scope.equals(\"%s\"))", nested_if_else_if, entry.getKey()));
        emitRecursiveStoreCalls(java_writer, element_to_model_interfaces, model_implementation);
        java_writer.endControlFlow();
        nested_if_else_if = else_if;
      }
      if (nested_if_else_if.equals(else_if)) {
        ModelData model_implementation = model_implementations.get(PRODUCTION).get(model.contract);
        if (shouldSkipModelImplementation(model_implementation)) continue;
        java_writer.beginControlFlow("else");
        emitRecursiveStoreCalls(java_writer, element_to_model_interfaces, model_implementation);
        java_writer.endControlFlow();
      }
      java_writer.endControlFlow();
      if_else_if_control = else_if;
    }
    java_writer.endMethod();
  }

  private static boolean shouldSkipModelImplementation(ModelData model_implementation) {
    return model_implementation == null
        || !model_implementation.should_serialize
        || model_implementation.parameters == null;
  }

  private void emitRecursiveStoreCalls(JavaWriter java_writer, Map<Element, ModelData> element_to_model_interfaces, ModelData model_implementation) throws IOException {
    for (Element parameter : model_implementation.parameters) {
      final ModelData sub_model = element_to_model_interfaces.get(_types.asElement(parameter.asType()));
      if (!sub_model.should_serialize) continue;
      java_writer.emitStatement("store(createModel(%s.class))", sub_model.contract);
    }
  }

  private static ModelData modelImplementation(ModelData model, Map<String, Map<Element, ModelData>> model_implementations,
      Map<Element, ModelData> models) {
    ModelData model_implementation = models.get(model.contract);
    if (model_implementation == null)
      model_implementation = model_implementations.containsKey(PRODUCTION)
          ? model_implementations.get(PRODUCTION).get(model.contract)
          : null;
    return model_implementation;
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
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage(package_name)
        .emitImports("javax.inject.Named",
            _SEGUE_CONTROLLER_CLASS,
            "com.squareup.otto.Bus",
            "dagger.Module",
            "dagger.Provides",
            "javax.inject.Singleton")
        .emitEmptyLine();
    final StringBuilder controller_list = new StringBuilder();
    for (ControllerData controller : controllers) {
      controller_list.append("<li>{@link ").append(controller.contract).append("}</li>\n");
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
        .beginType(class_name, _CLASS, public_modifier)
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
          .beginMethod(controller.contract + "", "provides" + controller.contract.getSimpleName(),
              EnumSet.noneOf(Modifier.class))
          .emitStatement("return new %s()", controller.implementation)
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
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage(package_name)
        .emitImports(JavaWriter.type(Finder.class))
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Data Source into {@link %s}'s %s.</p>", target, variable_name)
        .beginType(class_name, _CLASS, EnumSet.of(PUBLIC, FINAL))
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Data Source into {@link %s}'s %s.</p>\n" +
            "@param segue_controller The Segue Controller\n" +
            "@param finder The finder that specifies how to retrieve the context from the target\n" +
            "@param target The target of the injection", target, variable_name)
        .beginMethod(_VOID, "injectDataSource", EnumSet.of(PUBLIC, STATIC),
            JavaWriter.type(SegueController.class), _SEGUE_CONTROLLER,
            JavaWriter.type(Finder.class), "finder",
            target + "", _TARGET)
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
  private void generateModelModule(Writer writer, String package_name, List<ModelData> models, String class_name
          , boolean has_default_models, String default_model_module)
      throws IOException {
    final String file_tested = "_file_tested";
    final EnumSet<Modifier> private_final = EnumSet.of(PRIVATE, FINAL);
    final EnumSet<Modifier> private_modifier = EnumSet.of(PRIVATE);
    final JavaWriter java_writer = new JavaWriter(writer);
    final boolean is_default_module = !class_name.equals(_DEFAULT_MODEL_MODULE);
    java_writer.setCompressingTypes(true);
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage(package_name)
        .emitImports(_SEGUE_CONTROLLER_CLASS,
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
        .emitStaticImports(JavaWriter.type(Sets.class) + ".newHashSet"
                , JavaWriter.type(Sets.class) + ".union");
    if (has_default_models && is_default_module)
        java_writer.emitStaticImports(default_model_module + ".default_models");
    java_writer.emitEmptyLine();
    java_writer.emitJavadoc("<p>Module for injecting:\n" +
        "<ul>\n" +
        "%s" +
        "</ul></p>", modelList(models))
        .emitAnnotation(Module.class, ImmutableMap.of(
            "injects",
            "{\n" +
                "_SegueController.class" +
                "\n}",
            "overrides", is_default_module,
            "library", true,
            "complete", false))
        .beginType(class_name, _CLASS, EnumSet.of(PUBLIC));
    final List<String> model_exclusions = newArrayListWithCapacity(models.size());
    final String model_exclusion = "(Class) %s.class";
      
    if (class_name.equals(_DEFAULT_MODEL_MODULE)) {
      for (ModelData model : models) model_exclusions.add(String.format(model_exclusion, model.contract));
      java_writer.emitJavadoc("Set of production models")
                 .emitField(JavaWriter.type(Set.class, JavaWriter.type(Class.class)), "default_models"
                 , EnumSet.of(PUBLIC, STATIC, FINAL)
                 , "newHashSet(" + Joiner.on(",\n\t\t").join(model_exclusions) + ")");
    }
    java_writer.emitEmptyLine()
        .beginMethod(null, class_name, EnumSet.of(PUBLIC), JavaWriter.type(Timber.class), "log"
            , JavaWriter.type(SegueController.class), _SEGUE_CONTROLLER)
        .emitStatement("this.log = log")
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
      String[] provider_parameters = model.should_serialize
          ? new String[] {
          JavaWriter.type(GsonProvider.class)
          , "gson_provider"
          , "@Named(" + stringLiteral(model.contract + "")+ ") " + JavaWriter.type(GsonConverter.class)
          , "converter" }
          : new String[0];
      final String[] new_instance_format_parameters;
      if (model.parameters == null || model.parameters.isEmpty()) {
        new_instance_format_parameters = new String[] { java_writer.compressType(model.implementation
            + ""), "" };
      } else {
        final Set<String> provider_method_parameters = newLinkedHashSet(
            Arrays.asList(provider_parameters));
        final List<String> constructor_parameters = constructorParameters(model, provider_method_parameters);
        provider_parameters = provider_method_parameters.toArray(provider_parameters);
        new_instance_format_parameters = new String[] {
            java_writer.compressType(model.implementation + "")
            , Joiner.on(", ").join(constructor_parameters) };
      }
      java_writer.beginMethod(model.contract + "", "provides" + model.contract.getSimpleName(),
          EnumSet.noneOf(Modifier.class), provider_parameters);
      if (model.should_serialize) {
        java_writer.emitStatement("InputStream input_stream = null")
            .beginControlFlow("try")
            .beginControlFlow(String.format("if (!_%s%s)", model.variable_name, file_tested))
            .emitStatement("_%s%s = true", model.variable_name, file_tested)
            .emitStatement("input_stream = gson_provider.inputStreamFor(%s.class)", model.contract)
            .emitStatement("log.tag(_TAG).d(\"Restoring %s from input stream\")", java_writer.compressType(model.implementation
                + ""))
            .emitStatement("return (%s) converter.from(input_stream)", java_writer.compressType(model.implementation
                + ""))
            .nextControlFlow("else")
            .emitStatement("return new %s(%s)", (Object[]) new_instance_format_parameters)
            .endControlFlow()
            .nextControlFlow("catch (Exception _)")
            .emitStatement("log.tag(_TAG).d(\"Nothing to restore; creating model %s\")", model.contract)
            .emitStatement("return new %s(%s)", (Object[]) new_instance_format_parameters)
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
            .emitAnnotation(Named.class, stringLiteral(model.contract + ""))
            .beginMethod(JavaWriter.type(GsonConverter.class),
                "provides" + model.implementation.getSimpleName() + "Converter",
                EnumSet.noneOf(Modifier.class), JavaWriter.type(Gson.class), "gson")
            .emitStatement("return new %s(gson, %s.class)"
                , JavaWriter.type(GsonConverter.class, model.implementation + ""), model.implementation)
            .endMethod();
      } else {
        java_writer.emitStatement("return new %s(%s)", (Object[]) new_instance_format_parameters)
            .endMethod();
      }
    }
    java_writer.emitEmptyLine()
        .beginMethod(JavaWriter.type(Gson.class), "buildGson", EnumSet.of(PRIVATE)
            , JavaWriter.type(GsonBuilder.class), "gson_builder");
    final Joiner new_line_joiner = Joiner.on("%n");
    for (ModelData model : models) {
      if (is_default_module) model_exclusions.add(String.format(model_exclusion, model.contract));
      if (model.should_serialize) {
        java_writer.emitStatement("final InstanceCreator<%1$s> %1$s_creator = _segue_controller.instanceCreator(%2$s.class)"
            , java_writer.compressType(model.implementation + ""), model.contract)
            .emitStatement("gson_builder.registerTypeAdapter(%1$s.class, %1$s_creator)"
                , java_writer.compressType(model.implementation + ""));
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
        , "    private final Set<Class> _models = "
          + (class_name.equals(_DEFAULT_MODEL_MODULE)
            ? "default_models;"
            : (has_default_models? "union(" + "default_models, newHashSet(%s));" : "newHashSet(%s);"))
        , "})"), Joiner.on(",\n\t\t").join(model_exclusions))
        .emitStatement("return gson_builder.create()")
        .endMethod()
        .emitEmptyLine()
        .emitJavadoc("Log where messages are written")
        .emitField(JavaWriter.type(Timber.class), "log", private_final)
        .emitField(JavaWriter.type(String.class), "_TAG", EnumSet.of(PRIVATE, STATIC, FINAL), stringLiteral("Prestige"))
        .emitJavadoc("Segue Controller")
        .emitField(JavaWriter.type(SegueController.class), "_segue_controller", private_final);
    for (ModelData model : models)
      if (model.should_serialize)
        java_writer.emitField(JavaWriter.type(boolean.class), '_' + model.variable_name + file_tested, private_modifier, "false");
    java_writer.endType();
    java_writer.close();
  }

  private List<String> constructorParameters(ModelData model, Set<String> provider_method_parameters) {
    final List<String> constructor_parameters = newArrayList();
    for (VariableElement parameter : model.parameters) {
      final String type = _types.asElement(parameter.asType()) + "";
      final String parameter_string = parameter + "";
      if (!provider_method_parameters.contains(type)) {
        provider_method_parameters.add(type);
        provider_method_parameters.add(parameter_string);
      }
      constructor_parameters.add(parameter_string);
    }
    return constructor_parameters;
  }

  private static StringBuilder modelList(List<ModelData> models) {
    final String list_opener = "<li>{@link ";
    final String list_closer = "}</li>\n";
    final StringBuilder model_list = new StringBuilder();
    for (ModelData model : models)
      model_list.append(list_opener).append(model.contract).append(list_closer);
    return model_list;
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
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage(package_name)
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Models into {@link %s} and stores them for later use.</p>", class_name)
        .beginType(class_name, _CLASS, EnumSet.of(PUBLIC, FINAL))
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Models into {@link %s}.</p>\n" +
            "@param segue_controller The Segue Controller from which to retrieve Models\n" +
            "@param target The target of the injection", target)
        .beginMethod(_VOID, "injectModels", EnumSet.of(PUBLIC, STATIC),
            JavaWriter.type(SegueController.class), _SEGUE_CONTROLLER,
            ((TypeElement) target).getQualifiedName() + "", _TARGET);
    for (ModelInjectionData injection : injections)
      java_writer.emitStatement("target.%s = segue_controller.createModel(%s.class)", injection.variable_name,
          injection.variable.asType());
    java_writer.endMethod()
        .emitEmptyLine()
        .emitJavadoc("<p>Stores the Models from {@link %s}.</p>\n" +
            "@param segue_controller The Segue Controller used to store Models\n" +
            "@param source The source of models to store", target)
        .beginMethod(_VOID, "storeModels", EnumSet.of(PUBLIC, STATIC),
            newArrayList(JavaWriter.type(SegueController.class), _SEGUE_CONTROLLER,
                ((TypeElement) target).getQualifiedName() + "", "source"),
            newArrayList(JavaWriter.type(IOException.class)));
    for (ModelInjectionData injection : injections)
      java_writer.emitStatement("segue_controller.store(source.%s)", injection.variable_name);
    java_writer.endMethod()
        .endType()
        .emitEmptyLine();
    java_writer.close();
  }

  private void generateControllerPresentationFragmentInjector(Writer writer, String package_name,
      List<PresentationFragmentInjectionData> injections, String class_name, Element target) throws IOException {
    final JavaWriter java_writer = new JavaWriter(writer);
    java_writer.setCompressingTypes(true);
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage(package_name)
        .emitEmptyLine()
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>", class_name)
        .beginType(class_name, _CLASS, EnumSet.of(PUBLIC, FINAL))
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>\n" +
            "@param target The target of the injection\n" +
            "@param presentation_fragment The presentation fragment to inject\n" +
            "@param tag The tag that labels the presentation fragment", target)
        .beginMethod(_VOID, "attachPresentationFragment",
            EnumSet.of(PUBLIC, STATIC),
            _elements.getBinaryName((TypeElement) target) + "", _TARGET,
            JavaWriter.type(Object.class), "presentation_fragment",
            JavaWriter.type(String.class), "tag");
    final String control_format = "%s (presentation_fragment instanceof %s &&%n" +
        "\ttag.equals(\"%s\"))";
    if (!injections.isEmpty()) {
      final PresentationFragmentInjectionData injection = injections.get(0);
      java_writer.beginControlFlow(String.format(control_format, "if", injection.implementation, injection.tag))
          .emitStatement("target.%s = (%s) presentation_fragment", injection.variable_name,
              injection.variable.asType())
          .endControlFlow();
    }
    for (PresentationFragmentInjectionData injection : injections.subList(min(1, injections.size()), injections.size()))
      java_writer.beginControlFlow(String.format(control_format, "else if", injection.implementation, injection.tag))
          .emitStatement("target.%s = (%s) presentation_fragment", injection.variable_name,
              injection.variable.asType())
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
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage(package_name)
        .emitEmptyLine()
        .emitImports("android.app.FragmentManager",
            "android.app.Fragment",
            JavaWriter.type(Finder.class),
            "android.content.Context")
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>", target)
        .beginType(class_name, _CLASS, EnumSet.of(PUBLIC, FINAL))
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Presentation Fragments into {@link %s}.</p>\n" +
            "@param finder The finder that specifies how to retrieve the context\n" +
            "@param display The current display state\n" +
            "@param target The target of the injection", target)
        .beginMethod(_VOID, "injectPresentationFragments",
            EnumSet.of(PUBLIC, STATIC),
            JavaWriter.type(Finder.class), "finder",
            JavaWriter.type(int.class), "display",
            _elements.getBinaryName((TypeElement) target) + "", _TARGET);
    if (!injections.isEmpty()) {
      java_writer.emitStatement("final Context context = finder.findContext(target)")
          .emitStatement("final FragmentManager fragment_manager = finder.findFragmentManager(target)")
          .beginControlFlow("switch (display)");
      for (Map.Entry<Integer, List<PresentationFragmentInjectionData>> entry : injections.entrySet()) {
        java_writer.beginControlFlow("case " + entry.getKey() + ":");
        final StringBuilder transactions = new StringBuilder();
        for (PresentationFragmentInjectionData injection : entry.getValue()) {
          java_writer.emitStatement("target.%s = (%s) Fragment.instantiate(context, \"%s\")", injection.variable_name,
              injection.variable.asType(), injection.implementation);
          transactions.append("\t.add(")
              .append(injection.displays.get(entry.getKey()))
              .append(",\n")
              .append("\t(Fragment) \ttarget.")
              .append(injection.variable_name);
          if (!injection.tag.isEmpty())
            transactions.append(",\n\"").append(injection.tag).append("\"");
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
    java_writer.emitSingleLineComment(_HEADER_COMMENT)
        .emitPackage(package_name)
        .emitEmptyLine()
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Presentation into {@link %s}.</p>", class_name)
        .beginType(class_name, _CLASS, EnumSet.of(PUBLIC, FINAL))
        .emitEmptyLine()
        .emitJavadoc("<p>Injects the Presentation into {@link %s}.</p>\n" +
            "@param target The target of the injection\n" +
            "@param presentation The presentation to inject", target)
        .beginMethod(_VOID, "attachPresentation",
            EnumSet.of(PUBLIC, STATIC),
            _elements.getBinaryName((TypeElement) target) + "", _TARGET,
            JavaWriter.type(Object.class), "presentation");
    final String control_format = "if (presentation instanceof %s)";
    java_writer.beginControlFlow(String.format(Locale.US, control_format, injection.variable.asType()))
        .emitStatement("target.%s = (%s) presentation", injection.variable_name, injection.variable
            .asType())
        .endControlFlow();

    java_writer.endMethod()
        .endType()
        .emitEmptyLine();
    java_writer.close();
  }

  private final Filer _filer;
  private final Messager _messager;
  private final Elements _elements;
  private final Types _types;
  private static final String _CONVERTOR = "_converter";
  private static final String _DEFAULT_CONTROLLER_MODULE = LOWER_CAMEL.to(UPPER_CAMEL, PRODUCTION)
      + CONTROLLER_MODULE_SUFFIX;
  private static final String _DEFAULT_MODEL_MODULE = LOWER_CAMEL.to(UPPER_CAMEL, PRODUCTION)
      + MODEL_MODULE_SUFFIX;
  private static final String _CLASS = "class";
  private static final String _SEGUE_CONTROLLER_CLASS = "com.imminentmeals.prestige._SegueController";
  private static final String _VOID = "void";
  private static final String _SEGUE_CONTROLLER = "segue_controller";
  private static final String _TARGET = "target";
  private static final String _HEADER_COMMENT = "Generated code from Prestige. Modification is futile.";
}
