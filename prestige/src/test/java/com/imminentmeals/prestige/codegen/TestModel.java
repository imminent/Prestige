package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.FOUR;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SEVEN;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.THREE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestModel {

  @Test public void testInjectingModel() {
    final JavaFileObject controller =
        JavaFileObjects.forSourceString("test.ControllerWithModel", Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_INJECT_MODEL
            , _IMPORT_CONTROLLER_IMPLEMENTATION
            , _IMPORT_TEST
            , _CONTROLLER_IMPLEMENTATION_ANNOTATION
            , "public class ControllerWithModel implements ControllerInterface {"
            , "@InjectModel ModelInterface model;"
            , "}"));

    ASSERT.about(javaSources())
        .that(
            Arrays.asList(_MODEL_INTERFACE, _MODEL, _PRESENTATION_INTERFACE, _CONTROLLER_INTERFACE, controller))
        .processedWith(prestigeProcessors())
        .compilesWithoutError();
  }

  @Test public void testInjectingModelFailsIfInPresentation() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithModel", Joiner.on('\n').join(
              _PACKAGE_TEST
            , "import android.app.Activity;"
            , _IMPORT_INJECT_MODEL
            , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
            , "@PresentationImplementation"
            ,
            "public class PresentationWithModel extends Activity implements PresentationInterface {"
            , "@InjectModel ModelInterface model;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_MODEL_INTERFACE, _MODEL, _PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@InjectModel-annotated fields must not be specified in "
            + "@PresentationImplementation classes (%s)."
            , "test.PresentationWithModel"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testInjectingModelFailsIfInPresentationFragment() {
    final JavaFileObject presentation_fragment_interface =
        JavaFileObjects.forResource("PresentationFragmentInterface.java");
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("test.PresentationFragmentWithModel"
            , Joiner.on('\n').join(
              _PACKAGE_TEST
            , "import android.app.Fragment;"
            , _IMPORT_INJECT_MODEL
            , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
            , "@PresentationFragmentImplementation"
            ,
            "public class PresentationFragmentWithModel extends Fragment implements PresentationFragmentInterface {"
            , "@InjectModel ModelInterface model;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_MODEL_INTERFACE, _MODEL, presentation_fragment_interface,
            presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@InjectModel-annotated fields must not be specified in "
            + "@PresentationFragmentImplementation classes (%s)."
            , "test.PresentationFragmentWithModel"))
        .in(presentation_fragment)
        .onLine(SEVEN);
  }

  @Test public void testInjectingModelFailsIfNotModel() {
    final JavaFileObject controller =
        JavaFileObjects.forSourceString("test.ControllerWithInjectObject", Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_INJECT_MODEL
            , _IMPORT_CONTROLLER_IMPLEMENTATION
            , _IMPORT_TEST
            , _CONTROLLER_IMPLEMENTATION_ANNOTATION
            , "public class ControllerWithInjectObject implements ControllerInterface {"
            , "@InjectModel Object model;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PRESENTATION_INTERFACE, _CONTROLLER_INTERFACE, controller))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@InjectModel must be a Model (%s)."
            , "test.ControllerWithInjectObject.model"))
        .in(controller)
        .onLine(SEVEN);
  }

  @Test public void testInjectingModelFailsIfPrivate() {
    final JavaFileObject controller =
        JavaFileObjects.forSourceString("test.ControllerWithPrivateModel", Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_INJECT_MODEL
            , _IMPORT_CONTROLLER_IMPLEMENTATION
            , _IMPORT_TEST
            , _CONTROLLER_IMPLEMENTATION_ANNOTATION
            , "public class ControllerWithPrivateModel implements ControllerInterface {"
            , "@InjectModel private ModelInterface model;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_MODEL_INTERFACE, _MODEL, _PRESENTATION_INTERFACE, _CONTROLLER_INTERFACE, controller))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@InjectModel fields must not be private or static (%s)."
            , "test.ControllerWithPrivateModel.model"))
        .in(controller)
        .onLine(SEVEN);
  }

  @Test public void testInjectingModelFailsIfStatic() {
    final JavaFileObject controller =
        JavaFileObjects.forSourceString("test.ControllerWithStaticModel", Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_INJECT_MODEL
            , _IMPORT_CONTROLLER_IMPLEMENTATION
            , _IMPORT_TEST
            , _CONTROLLER_IMPLEMENTATION_ANNOTATION
            , "public class ControllerWithStaticModel implements ControllerInterface {"
            , "@InjectModel static ModelInterface model;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_MODEL_INTERFACE, _MODEL, _PRESENTATION_INTERFACE, _CONTROLLER_INTERFACE, controller))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@InjectModel fields must not be private or static (%s)."
            , "test.ControllerWithStaticModel.model"))
        .in(controller)
        .onLine(SEVEN);
  }

  @Test public void testModelFailsIfClass() {
    final JavaFileObject model = JavaFileObjects.forSourceString("ClassModel", Joiner.on('\n').join(
          _IMPORT_MODEL
        , _MODEL_ANNOTATION
        , "public class ClassModel { }"));

    ASSERT.about(javaSource())
        .that(model)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format("@Model annotation may only be specified on interfaces (%s)."
                , "ClassModel"))
        .in(model)
        .onLine(THREE);
  }

  @Test public void testModelFailsIfNotPublic() {
    JavaFileObject model =
        JavaFileObjects.forSourceString("PackageProtectedModel", Joiner.on('\n').join(
              _IMPORT_MODEL
            , _MODEL_ANNOTATION
            , "interface PackageProtectedModel { }"));

    ASSERT.about(javaSource())
        .that(model)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@Model interface must be public (%s)."
            , "PackageProtectedModel"))
        .in(model)
        .onLine(THREE);

    model = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
          _IMPORT_MODEL
        , "public class Test {"
        , _MODEL_ANNOTATION
        , "private static interface PrivateModel { }"
        , "}"));

    ASSERT.about(javaSource())
        .that(model)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@Model interface must be public (%s)."
            , "Test.PrivateModel"))
        .in(model)
        .onLine(FOUR);

    model = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
          _IMPORT_MODEL
        , "public class Test {"
        , _MODEL_ANNOTATION
        , "protected static interface PrivateModel { }"
        , "}"));

    ASSERT.about(javaSource())
        .that(model)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@Model interface must be public (%s)."
            , "Test.PrivateModel"))
        .in(model)
        .onLine(FOUR);
  }

  private static final JavaFileObject _MODEL_INTERFACE =
      JavaFileObjects.forResource("ModelInterface.java");
  private static final JavaFileObject _MODEL = JavaFileObjects.forResource("TestModel.java");
  private static final JavaFileObject _PRESENTATION_INTERFACE =
      JavaFileObjects.forResource("PresentationInterface.java");
  private static final JavaFileObject _CONTROLLER_INTERFACE =
      JavaFileObjects.forResource("ControllerInterface.java");
  private static final String _PACKAGE_TEST = "package test;";
  private static final String _IMPORT_INJECT_MODEL =
      "import com.imminentmeals.prestige.annotations.InjectModel;";
  private static final String _IMPORT_CONTROLLER_IMPLEMENTATION =
      "import com.imminentmeals.prestige.annotations.ControllerImplementation;";
  private static final String _IMPORT_TEST =
      "import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;";
  private static final String _CONTROLLER_IMPLEMENTATION_ANNOTATION =
      "@ControllerImplementation(TEST)";
  private static final String _IMPORT_MODEL =
      "import com.imminentmeals.prestige.annotations.Model;";
  private static final String _MODEL_ANNOTATION = "@Model";
}
