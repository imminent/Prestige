package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.FIVE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.FOUR;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.ONE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SEVEN;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.THREE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.TWO;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestPresentationFragment {

  @Test public void testInjectingPresentationFragment() {
    final JavaFileObject controller_interface =
        JavaFileObjects.forResource("ControllerInterface.java");
    final JavaFileObject controller =
        JavaFileObjects.forSourceString("test.ControllerWithPresentationFragment"
            , Joiner.on('\n').join(
              _PACKAGE_TEST
            , "import com.imminentmeals.prestige.annotations.ControllerImplementation;"
            , _IMPORT_INJECT_PRESENTATION_FRAGMENT
            , "import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;"
            , "@ControllerImplementation(TEST)"
            , "public class ControllerWithPresentationFragment implements ControllerInterface {"
            ,
            "@InjectPresentationFragment(tag = \"\") PresentationFragmentInterface presentation_fragment;"
            , "}"));
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithPresentationFragment"
            , Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _IMPORT_INJECT_PRESENTATION_FRAGMENT
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithPresentationFragment extends Activity implements PresentationInterface {"
            ,
            "@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;"
            , "}"));
    final JavaFileObject other_presentation_fragment_interface = JavaFileObjects.forSourceString(
        "test.OtherPresentationFragmentInterface", Joiner.on('\n').join(
          _PACKAGE_TEST
        , _IMPORT_PRESENTATION_FRAGMENT
        , _PRESENTATION_FRAGMENT_ANNOTATION
        , "public interface OtherPresentationFragmentInterface { }"));
    final JavaFileObject other_presentation_fragment = JavaFileObjects.forSourceString(
        "test.PresentationFragmentWithPresentationFragment", Joiner.on('\n').join(
          _PACKAGE_TEST
        , "import android.app.Fragment;"
        , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
        , _IMPORT_INJECT_PRESENTATION_FRAGMENT
        , "@PresentationFragmentImplementation"
        ,
        "public class PresentationFragmentWithPresentationFragment extends Fragment implements OtherPresentationFragmentInterface {"
        ,
        "@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;"
        , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(
              _PRESENTATION_INTERFACE
            , _PRESENTATION_FRAGMENT_INTERFACE
            , _PRESENTATION_FRAGMENT
            , controller_interface
            , controller))
        .processedWith(prestigeProcessors())
        .compilesWithoutError();

    ASSERT.about(javaSources())
        .that(Arrays.asList(
              _PRESENTATION_INTERFACE
            , _PRESENTATION_FRAGMENT_INTERFACE
            , _PRESENTATION_FRAGMENT
            , presentation))
        .processedWith(prestigeProcessors())
        .compilesWithoutError();

    ASSERT.about(javaSources())
        .that(Arrays.asList(
              _PRESENTATION_INTERFACE
            , _PRESENTATION_FRAGMENT_INTERFACE
            , _PRESENTATION_FRAGMENT
            , other_presentation_fragment_interface
            , other_presentation_fragment))
        .processedWith(prestigeProcessors())
        .compilesWithoutError();
  }

  @Test public void testInjectPresentationFragmentFailsIfInNotAnnotatedClass() {
    final JavaFileObject container =
        JavaFileObjects.forSourceString("test.NotAnnotatedClass", Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_INJECT_PRESENTATION_FRAGMENT
            , "public class NotAnnotatedClass extends Activity implements PresentationInterface {"
            ,
            "@InjectPresentationFragment(manual = true) PresentationFragmentInterface presentation_fragment;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(
              _PRESENTATION_INTERFACE
            , _PRESENTATION_FRAGMENT_INTERFACE
            , _PRESENTATION_FRAGMENT
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

  @Test public void testInjectingPresentationFragmentFailsIfNotPresentationFragment() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithPresentationFragment_broken"
            , Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _IMPORT_INJECT_PRESENTATION_FRAGMENT
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithPresentationFragment_broken extends Activity implements PresentationInterface {"
            , "@InjectPresentationFragment(manual = true) Object presentation_fragment;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format("@InjectPresentationFragment must be a @PresentationFragment (%s)"
                , "test.PresentationWithPresentationFragment_broken.presentation_fragment"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testInjectingPresentationFragmentFailsIfPrivate() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithPrivatePresentationFragment"
            , Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _IMPORT_INJECT_PRESENTATION_FRAGMENT
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithPrivatePresentationFragment extends Activity implements PresentationInterface {"
            ,
            "@InjectPresentationFragment(manual = true) private PresentationFragmentInterface presentation_fragment;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(
              _PRESENTATION_INTERFACE
            , _PRESENTATION_FRAGMENT_INTERFACE
            , _PRESENTATION_FRAGMENT
            , presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format("@InjectPresentationFragment fields must not be private or static (%s)."
                , "test.PresentationWithPrivatePresentationFragment.presentation_fragment"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testInjectingPresentationFragmentFailsIfStatic() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithStaticPresentationFragment"
            , Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _IMPORT_INJECT_PRESENTATION_FRAGMENT
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithStaticPresentationFragment extends Activity implements PresentationInterface {"
            ,
            "@InjectPresentationFragment(manual = true) private PresentationFragmentInterface presentation_fragment;"
            , "}"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(
              _PRESENTATION_INTERFACE
            , _PRESENTATION_FRAGMENT_INTERFACE
            , _PRESENTATION_FRAGMENT
            , presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format("@InjectPresentationFragment fields must not be private or static (%s)."
                , "test.PresentationWithStaticPresentationFragment.presentation_fragment"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testPresentationFragmentFailsIfNotInterface() {
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("test.NotInterfacePresentationFragment"
            , Joiner.on('\n').join(
              _PACKAGE_TEST
            , _IMPORT_PRESENTATION_FRAGMENT
            , _PRESENTATION_FRAGMENT_ANNOTATION
            , "public class NotInterfacePresentationFragment { }"));

    ASSERT.about(javaSource())
        .that(presentation_fragment)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format(
            "@PresentationFragment annotation may only be specified on interfaces (%s)."
            , "test.NotInterfacePresentationFragment"))
        .in(presentation_fragment)
        .onLine(FOUR);
  }

  @Test public void testPresentationFragmentFailsIfNotPublic() {
    JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("PackageProtectedPresentationFragment"
            , Joiner.on('\n').join(
            _IMPORT_PRESENTATION_FRAGMENT
            , _PRESENTATION_FRAGMENT_ANNOTATION
            , "interface PackageProtectedPresentationFragment { }"));

    ASSERT.about(javaSource())
        .that(presentation_fragment)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@PresentationFragment interfaces must be public (%s)."
            , "PackageProtectedPresentationFragment"))
        .in(presentation_fragment)
        .onLine(THREE);

    presentation_fragment = JavaFileObjects.forSourceString(_TEST_CLASS
        , Joiner.on('\n').join(
        _IMPORT_PRESENTATION_FRAGMENT
        , _BEGIN_TEST_CLASS
        , _PRESENTATION_FRAGMENT_ANNOTATION
        , "private interface PrivatePresentationFragment { }"
        , "}"));

    ASSERT.about(javaSource())
        .that(presentation_fragment)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@PresentationFragment interfaces must be public (%s)."
            , "Test.PrivatePresentationFragment"))
        .in(presentation_fragment)
        .onLine(FOUR);

    presentation_fragment = JavaFileObjects.forSourceString(_TEST_CLASS, Joiner.on('\n').join(
        _IMPORT_PRESENTATION_FRAGMENT
        , _BEGIN_TEST_CLASS
        , _PRESENTATION_FRAGMENT_ANNOTATION
        , "protected interface ProtectedPresentationFragment { }"
        , "}"));

    ASSERT.about(javaSource())
        .that(presentation_fragment)
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@PresentationFragment interfaces must be public (%s)."
            , "Test.ProtectedPresentationFragment"))
        .in(presentation_fragment)
        .onLine(FOUR);
  }

  @Test public void testProtocolFailsIfNotInterface() {
    final JavaFileObject protocol = JavaFileObjects.forSourceString("NotClassProtocol"
        , "public class NotClassProtocol { }");
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("PresentationFragmentWithProtocol_broken"
            , Joiner.on('\n').join(
              "import android.app.Fragment;"
            , _IMPORT_PRESENTATION_FRAGMENT
            , "@PresentationFragment(protocol = NotClassProtocol.class)"
            , "public interface PresentationFragmentWithProtocol_broken { }"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(protocol, presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format("@PresentationFragment Protocol must be an interface (%s)."
                , "NotClassProtocol"))
        .in(protocol)
        .onLine(ONE);
  }

  @Test public void testProtocolFailsIfNotPublic() {
    final String format = Joiner.on('\n').join(
        _IMPORT_PRESENTATION_FRAGMENT
        , "@PresentationFragment(protocol = %s.class)"
        , "public interface PresentationFragmentWithProtocol_broken { }");
    JavaFileObject protocol = JavaFileObjects.forSourceString("PackageProtectedProtocol"
        , "interface PackageProtectedProtocol { }");
    JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("PresentationFragmentWithProtocol_broken"
            , String.format(format, "PackageProtectedProtocol"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(protocol, presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@PresentationFragment Protocol must be public (%s)."
            , "PackageProtectedProtocol"))
        .in(protocol)
        .onLine(ONE);

    protocol = JavaFileObjects.forSourceString(_TEST_CLASS, Joiner.on('\n').join(
        _IMPORT_PRESENTATION_FRAGMENT
        , _BEGIN_TEST_CLASS
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
        .onLine(THREE);

    protocol = JavaFileObjects.forSourceString(_TEST_CLASS, Joiner.on('\n').join(
          _BEGIN_TEST_CLASS
        , "protected interface ProtectedProtocol { }"
        , "}"));
    presentation_fragment =
        JavaFileObjects.forSourceString("PresentationFragmentWithProtocol_broken"
            , String.format(format, "Test.ProtectedProtocol"));

    ASSERT.about(javaSources())
        .that(Arrays.asList(protocol, presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@PresentationFragment Protocol must be public (%s)."
            , "Test.ProtectedProtocol"))
        .in(protocol)
        .onLine(TWO);
  }

  private static final JavaFileObject _PRESENTATION_INTERFACE =
      JavaFileObjects.forResource("PresentationInterface.java");
  private static final JavaFileObject _PRESENTATION_FRAGMENT_INTERFACE =
      JavaFileObjects.forResource("PresentationFragmentInterface.java");
  private static final JavaFileObject _PRESENTATION_FRAGMENT =
      JavaFileObjects.forResource("TestPresentationFragment.java");
  private static final String _PACKAGE_TEST = "package test;";
  private static final String _IMPORT_INJECT_PRESENTATION_FRAGMENT =
      "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;";
  private static final String _IMPORT_ACTIVITY = "import android.app.Activity;";
  private static final String _IMPORT_PRESENTATION_IMPLEMENTATION =
      "import com.imminentmeals.prestige.annotations.PresentationImplementation;";
  private static final String _PRESENTATION_IMPLEMENTATION_ANNOTATION =
      "@PresentationImplementation";
  private static final String _PRESENTATION_FRAGMENT_ANNOTATION = "@PresentationFragment";
  private static final String _TEST_CLASS = "Test";
  private static final String _BEGIN_TEST_CLASS = "public class Test {";
  private static final String _IMPORT_PRESENTATION_FRAGMENT =
      "import com.imminentmeals.prestige.annotations.PresentationFragment;";
}
