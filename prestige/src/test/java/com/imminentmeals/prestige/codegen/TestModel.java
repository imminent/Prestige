package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.FOUR;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SEVEN;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.THREE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestModel {

    @Test
    public void testInjectingModel() {
        final JavaFileObject model_interface = JavaFileObjects.forResource("ModelInterface.java");
        final JavaFileObject model = JavaFileObjects.forResource("TestModel.java");
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller_interface = JavaFileObjects.forResource("ControllerInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.ControllerWithModel", Joiner.on('\n').join(
                "package test;"
                , "import com.imminentmeals.prestige.annotations.InjectModel;"
                , "import com.imminentmeals.prestige.annotations.ControllerImplementation;"
                , "import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;"
                , "@ControllerImplementation(TEST)"
                , "public class ControllerWithModel implements ControllerInterface {"
                , "@InjectModel ModelInterface model;"
                , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(model_interface, model, presentation, controller_interface, controller))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();
    }

    @Test
    public void testInjectingModelFailsIfInPresentation() {
        final JavaFileObject model_interface = JavaFileObjects.forResource("ModelInterface.java");
        final JavaFileObject model = JavaFileObjects.forResource("TestModel.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithModel", Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.InjectModel;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "@PresentationImplementation"
              , "public class PresentationWithModel extends Activity implements PresentationInterface {"
              , "@InjectModel ModelInterface model;"
              , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(model_interface, model, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectModel-annotated fields must not be specified in "
                                               + "@PresentationImplementation classes (%s)."
                                               , "test.PresentationWithModel"))
              .in(presentation)
              .onLine(SEVEN);
    }

    @Test
    public void testInjectingModelFailsIfInPresentationFragment() {
        final JavaFileObject model_interface = JavaFileObjects.forResource("ModelInterface.java");
        final JavaFileObject model = JavaFileObjects.forResource("TestModel.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("test.PresentationFragmentWithModel"
                                                                                   , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.InjectModel;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
              , "@PresentationFragmentImplementation"
              , "public class PresentationFragmentWithModel extends Fragment implements PresentationFragmentInterface {"
              , "@InjectModel ModelInterface model;"
              , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(model_interface, model, presentation_fragment_interface, presentation_fragment))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectModel-annotated fields must not be specified in "
                      + "@PresentationFragmentImplementation classes (%s)."
                      , "test.PresentationFragmentWithModel"))
              .in(presentation_fragment)
              .onLine(SEVEN);
    }

    @Test
    public void testInjectingModelFailsIfNotModel() {
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller_interface = JavaFileObjects.forResource("ControllerInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.ControllerWithInjectObject", Joiner.on('\n').join(
                "package test;"
              , "import com.imminentmeals.prestige.annotations.InjectModel;"
              , "import com.imminentmeals.prestige.annotations.ControllerImplementation;"
              , "import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;"
              , "@ControllerImplementation(TEST)"
              , "public class ControllerWithInjectObject implements ControllerInterface {"
              , "@InjectModel Object model;"
              , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(presentation, controller_interface, controller))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectModel must be a Model (%s)."
                                               , "test.ControllerWithInjectObject.model"))
              .in(controller)
              .onLine(SEVEN);
    }

    @Test
    public void testInjectingModelFailsIfPrivate() {
        final JavaFileObject model_interface = JavaFileObjects.forResource("ModelInterface.java");
        final JavaFileObject model = JavaFileObjects.forResource("TestModel.java");
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller_interface = JavaFileObjects.forResource("ControllerInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.ControllerWithPrivateModel", Joiner.on('\n').join(
                "package test;"
                , "import com.imminentmeals.prestige.annotations.InjectModel;"
                , "import com.imminentmeals.prestige.annotations.ControllerImplementation;"
                , "import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;"
                , "@ControllerImplementation(TEST)"
                , "public class ControllerWithPrivateModel implements ControllerInterface {"
                , "@InjectModel private ModelInterface model;"
                , "}"));

        ASSERT.about(javaSources())
                .that(Arrays.asList(model_interface, model, presentation, controller_interface, controller))
                .processedWith(prestigeProcessors())
                .failsToCompile()
                .withErrorContaining(String.format("@InjectModel fields must not be private or static (%s)."
                                                 , "test.ControllerWithPrivateModel.model"))
                .in(controller)
                .onLine(SEVEN);
    }

    @Test
    public void testInjectingModelFailsIfStatic() {
        final JavaFileObject model_interface = JavaFileObjects.forResource("ModelInterface.java");
        final JavaFileObject model = JavaFileObjects.forResource("TestModel.java");
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller_interface = JavaFileObjects.forResource("ControllerInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.ControllerWithStaticModel", Joiner.on('\n').join(
                "package test;"
                , "import com.imminentmeals.prestige.annotations.InjectModel;"
                , "import com.imminentmeals.prestige.annotations.ControllerImplementation;"
                , "import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;"
                , "@ControllerImplementation(TEST)"
                , "public class ControllerWithStaticModel implements ControllerInterface {"
                , "@InjectModel static ModelInterface model;"
                , "}"));

        ASSERT.about(javaSources())
                .that(Arrays.asList(model_interface, model, presentation, controller_interface, controller))
                .processedWith(prestigeProcessors())
                .failsToCompile()
                .withErrorContaining(String.format("@InjectModel fields must not be private or static (%s)."
                        , "test.ControllerWithStaticModel.model"))
                .in(controller)
                .onLine(SEVEN);
    }

    @Test
    public void testModelFailsIfClass() {
        final JavaFileObject model = JavaFileObjects.forSourceString("ClassModel", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Model;"
              , "@Model"
              , "public class ClassModel { }"));

        ASSERT.about(javaSource())
              .that(model)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Model annotation may only be specified on interfaces (%s)."
                                               , "ClassModel"))
              .in(model)
              .onLine(THREE);
    }

    @Test
    public void testModelFailsIfNotPublic() {
        JavaFileObject model = JavaFileObjects.forSourceString("PackageProtectedModel", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Model;"
                , "@Model"
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
                "import com.imminentmeals.prestige.annotations.Model;"
                , "public class Test {"
                , "@Model"
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
                "import com.imminentmeals.prestige.annotations.Model;"
                , "public class Test {"
                , "@Model"
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
}
