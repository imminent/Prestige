package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

@RunWith(JUnit4.class)
public class TestController {

    @Test
    public void testControllerFailsIfMissingPresentationProtocol() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject controller = JavaFileObjects.forResource("ControllerForPresentationWithProtocolInterface_broken.java");

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation, controller))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Controller is required to implement Protocol %s by its Presentation (%s)."
                                               , "test.Protocol", "test.PresentationWithProtocolInterface"))
              .in(controller)
              .onLine(6);
    }

    @Test
    public void testControllerForPresentationWithProtocol() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject controller = JavaFileObjects.forResource("ControllerForPresentationWithProtocolInterface.java");

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation, controller))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();
    }

    @Test
    public void testControllerFailsIfNotInterface() {
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.NotInterfaceController", Joiner.on('\n').join(
                "package test;"
              , "import com.imminentmeals.prestige.annotations.Controller;"
              , "@Controller(presentation = PresentationInterface.class)"
              , "public class NotInterfaceController{ }"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(presentation, controller))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Controller annotation may only be specified on interfaces (%s)."
                                               , "test.NotInterfaceController"))
              .in(controller)
              .onLine(4);
    }

    @Test
    public void testControllerFailsIfPackageProtected() {
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.PackageProtectedController", Joiner.on('\n').join(
                "package test;"
              , "import com.imminentmeals.prestige.annotations.Controller;"
              , "@Controller(presentation = PresentationInterface.class)"
              , "/* package */interface PackageProtectedController { }"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(presentation, controller))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Controller interface must be public (%s)."
                                               , "test.PackageProtectedController"))
              .in(controller)
              .onLine(4);
    }

    @Test
    public void testControllerFailsIfPrivate() {
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.Test", Joiner.on('\n').join(
                "package test;"
                , "import com.imminentmeals.prestige.annotations.Controller;"
                , "public class Test {"
                , "@Controller(presentation = PresentationInterface.class)"
                , "private interface PrivateController { }"
                , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(presentation, controller))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Controller interface must be public (%s)."
                                               , "test.Test.PrivateController"))
              .in(controller)
              .onLine(5);
    }

    @Test
    public void testControllerFailsIfPresentationNotPresentation() {
        final JavaFileObject explicit = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Controller;"
              , "public class Test {"
              , "public interface ExplicitNotAnnotatedPresentation { }"
              , "@Controller(presentation = ExplicitNotAnnotatedPresentation.class)"
              , "public interface ExplicitNotAnnotatedController { }"
              , "}"
        ));
        final JavaFileObject implicit = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Controller;"
              , "public class Test {"
              , "public interface ImplicitNotAnnotatedPresentation { }"
              , "@Controller"
              , "public interface ImplicitNotAnnotatedController { }"

              , "}"
        ));

        ASSERT.about(javaSource())
              .that(explicit)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Controller Presentation must be an @Presentation (%s)."
                                               , "Test.ExplicitNotAnnotatedPresentation"))
              .in(explicit)
              .onLine(5);

        ASSERT.about(javaSource())
              .that(implicit)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("No @Presentation-annotated %s found, implicitly required by %s"
                                               , "ImplicitNotAnnotatedPresentation", "Test.ImplicitNotAnnotatedController"))
              .in(implicit)
              .onLine(5);
    }

    @Test
    public void testControllerWithImplicitPresentation() {
        final JavaFileObject source = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Controller;"
              , "import com.imminentmeals.prestige.annotations.Presentation;"
              , "public class Test {"
              , "@Presentation"
              , "public interface ImplicitlyDefinedPresentation { }"
              , "@Controller"
              , "public interface ImplicitlyDefinedController { }"
              , "}"
        ));

        ASSERT.about(javaSource())
                .that(source)
                .processedWith(prestigeProcessors())
                .compilesWithoutError();
    }
}
