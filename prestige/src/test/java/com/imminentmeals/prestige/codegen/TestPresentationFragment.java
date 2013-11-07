package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.FIVE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.FOUR;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SEVEN;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.THREE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestPresentationFragment {

    @Test
    public void testInjectingPresentationFragment() {
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forResource("TestPresentationFragment.java");
        final JavaFileObject controller_interface = JavaFileObjects.forResource("ControllerInterface.java");
        final JavaFileObject controller = JavaFileObjects.forSourceString("test.ControllerWithPresentationFragment"
                                                                        , Joiner.on('\n').join(
                "package test;"
              , "import com.imminentmeals.prestige.annotations.ControllerImplementation;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;"
              , "@ControllerImplementation(TEST)"
              , "public class ControllerWithPresentationFragment implements ControllerInterface {"
              , "@InjectPresentationFragment(tag = \"\") PresentationFragmentInterface presentation_fragment;"
              , "}"));
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithPresentationFragment"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "@PresentationImplementation"
              , "public class PresentationWithPresentationFragment extends Activity implements PresentationInterface {"
              , "@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;"
              , "}"));
        final JavaFileObject other_presentation_fragment_interface = JavaFileObjects.forSourceString(
                "test.OtherPresentationFragmentInterface", Joiner.on('\n').join(
                "package test;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "@PresentationFragment"
              , "public interface OtherPresentationFragmentInterface { }"));
        final JavaFileObject other_presentation_fragment = JavaFileObjects.forSourceString(
                "test.PresentationFragmentWithPresentationFragment", Joiner.on('\n').join(
                "package test;"
              , "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "@PresentationFragmentImplementation"
              , "public class PresentationFragmentWithPresentationFragment extends Fragment implements OtherPresentationFragmentInterface {"
              , "@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;"
              , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(
                      presentation_interface
                    , presentation_fragment_interface
                    , presentation_fragment
                    , controller_interface
                    , controller))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();

        ASSERT.about(javaSources())
              .that(Arrays.asList(
                      presentation_interface
                    , presentation_fragment_interface
                    , presentation_fragment
                    , presentation))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();

        ASSERT.about(javaSources())
              .that(Arrays.asList(
                      presentation_interface
                    , presentation_fragment_interface
                    , presentation_fragment
                    , other_presentation_fragment_interface
                    , other_presentation_fragment))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();
    }

    @Test
    public void testInjectPresentationFragmentFailsIfInNotAnnotatedClass() {
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forResource("TestPresentationFragment.java");
        final JavaFileObject container = JavaFileObjects.forSourceString("test.NotAnnotatedClass", Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "public class NotAnnotatedClass extends Activity implements PresentationInterface {"
              , "@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;"
              , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(
                      presentation_interface
                    , presentation_fragment_interface
                    , presentation_fragment
                    , container))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(
                      String.format("@InjectPresentationFragment-annotated fields must be specified "
                                  + "in @PresentationImplementation or @PresentationFragmentImplementation "
                                  + "or @ControllerImplementation classes (%s)."
                                  , "test.NotAnnotatedClass.presentation_fragment"))
              .in(container)
              .onLine(FIVE);
    }

    @Test
    public void testInjectingPresentationFragmentFailsIfNotPresentationFragment() {
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithPresentationFragment_broken"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "@PresentationImplementation"
              , "public class PresentationWithPresentationFragment_broken extends Activity implements PresentationInterface {"
              , "@InjectPresentationFragment(manual = true) Object presentation_fragment;"
              , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectPresentationFragment must be a @PresentationFragment (%s)"
                                               , "test.PresentationWithPresentationFragment_broken.presentation_fragment"))
              .in(presentation)
              .onLine(SEVEN);
    }

    @Test
    public void testInjectingPresentationFragmentFailsIfPrivate() {
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forResource("TestPresentationFragment.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithPrivatePresentationFragment"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "@PresentationImplementation"
              , "public class PresentationWithPrivatePresentationFragment extends Activity implements PresentationInterface {"
              , "@InjectPresentationFragment(manual = true) private PresentationFragmentInterface presentation_fragment;"
              , "}"));


        ASSERT.about(javaSources())
              .that(Arrays.asList(
                        presentation_interface
                      , presentation_fragment_interface
                      , presentation_fragment
                      , presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectPresentationFragment fields must not be private or static (%s)."
                                               , "test.PresentationWithPrivatePresentationFragment.presentation_fragment"))
              .in(presentation)
              .onLine(SEVEN);
    }

    @Test
    public void testInjectingPresentationFragmentFailsIfStatic() {
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forResource("TestPresentationFragment.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithStaticPresentationFragment"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "@PresentationImplementation"
              , "public class PresentationWithStaticPresentationFragment extends Activity implements PresentationInterface {"
              , "@InjectPresentationFragment(manual = true) private PresentationFragmentInterface presentation_fragment;"
              , "}"));


        ASSERT.about(javaSources())
              .that(Arrays.asList(
                      presentation_interface
                      , presentation_fragment_interface
                      , presentation_fragment
                      , presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectPresentationFragment fields must not be private or static (%s)."
                      , "test.PresentationWithStaticPresentationFragment.presentation_fragment"))
              .in(presentation)
              .onLine(SEVEN);
    }

    @Test
    public void testPresentationFragmentFailsIfNotInterface() {
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("test.NotInterfacePresentationFragment"
                                                                                   , Joiner.on('\n').join(
                "package test;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "@PresentationFragment"
              , "public class NotInterfacePresentationFragment { }"));

        ASSERT.about(javaSource())
              .that(presentation_fragment)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@PresentationFragment annotation may only be specified on interfaces (%s)."
                                               , "test.NotInterfacePresentationFragment"))
              .in(presentation_fragment)
              .onLine(FOUR);
    }

    @Test
    public void testPresentationFragmentFailsIfNotPublic() {
        JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("PackageProtectedPresentationFragment"
                                                                                   , Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "@PresentationFragment"
              , "interface PackageProtectedPresentationFragment { }"));

        ASSERT.about(javaSource())
              .that(presentation_fragment)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@PresentationFragment interfaces must be public (%s)."
                                               , "PackageProtectedPresentationFragment"))
              .in(presentation_fragment)
              .onLine(THREE);

        presentation_fragment = JavaFileObjects.forSourceString("Test"
                                                              , Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "public class Test {"
              , "@PresentationFragment"
              , "private interface PrivatePresentationFragment { }"
              , "}"));

        ASSERT.about(javaSource())
              .that(presentation_fragment)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@PresentationFragment interfaces must be public (%s)."
                                               , "Test.PrivatePresentationFragment"))
              .in(presentation_fragment)
              .onLine(4);

        presentation_fragment = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "public class Test {"
              , "@PresentationFragment"
              , "protected interface ProtectedPresentationFragment { }"
              , "}"));

        ASSERT.about(javaSource())
                .that(presentation_fragment)
                .processedWith(prestigeProcessors())
                .failsToCompile()
                .withErrorContaining(String.format("@PresentationFragment interfaces must be public (%s)."
                        , "Test.ProtectedPresentationFragment"))
                .in(presentation_fragment)
                .onLine(4);
    }

    @Test
    public void testProtocolFailsIfNotInterface() {
        final JavaFileObject protocol = JavaFileObjects.forSourceString("NotClassProtocol"
               , "public class NotClassProtocol { }");
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("PresentationFragmentWithProtocol_broken"
                                                                                   , Joiner.on('\n').join(
                "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "@PresentationFragment(protocol = NotClassProtocol.class)"
              , "public interface PresentationFragmentWithProtocol_broken { }"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_fragment))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@PresentationFragment Protocol must be an interface (%s)."
                                               , "NotClassProtocol"))
              .in(presentation_fragment)
              .onLine(4);
    }

    @Test
    public void testProtocolFailsIfNotPublic() {
        final String format = Joiner.on('\n').join(
               "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "@PresentationFragment(protocol = %s.class)"
              , "public interface PresentationFragmentWithProtocol_broken { }");
        JavaFileObject protocol = JavaFileObjects.forSourceString("PackageProtectedProtocol"
                , "interface PackageProtectedProtocol { }");
        JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("PresentationFragmentWithProtocol_broken"
                                                                             , String.format(format, "PackageProtectedProtocol"));

        ASSERT.about(javaSources())
                .that(Arrays.asList(protocol, presentation_fragment))
                .processedWith(prestigeProcessors())
                .failsToCompile()
                .withErrorContaining(String.format("@PresentationFragment Protocol must be public (%s)."
                                                 , "PackageProtectedProtocol"))
                .in(presentation_fragment)
                .onLine(3);

        protocol = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.PresentationFragment;"
              , "public class Test {"
              , "private interface PrivateProtocol { }"
              , "@PresentationFragment(protocol = PrivateProtocol.class)"
              , "public interface PresentationFragmentWithProtocol_broken { }"
              , "}"));

        ASSERT.about(javaSource())
                .that(protocol)
                .processedWith(prestigeProcessors())
                .failsToCompile()
                .withErrorContaining(String.format("@PresentationFragment Protocol must be public (%s)."
                        , "Test.PrivateProtocol"))
                .in(protocol)
                .onLine(FIVE);

        protocol = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "public class Test {"
              , "protected interface ProtectedProtocol { }"
              , "}"));
        presentation_fragment = JavaFileObjects.forSourceString("PresentationFragmentWithProtocol_broken"
                                                              , String.format(format, "Test.ProtectedProtocol"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_fragment))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@PresentationFragment Protocol must be public (%s)."
                                               , "Test.ProtectedProtocol"))
              .in(presentation_fragment)
              .onLine(3);
    }
}
