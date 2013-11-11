package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SEVEN;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestDataSource {

  @Test public void testPresentationDataSourceFailsIfNotProtocol() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.NonProtocolDataSourcePresentation",
            Joiner.on('\n').join(
                _PACKAGE_TEST
                , _IMPORT_ACTIVITY
                , _IMPORT_IMPORT_DATA_SOURCE
                , _IMPORT_PRESENTATION_IMPLEMENTATION
                , _PRESENTATION_IMPLEMENTATION_ANNOTATION
                ,
                "public class NonProtocolDataSourcePresentation extends Activity implements PresentationWithProtocolInterface {"
                , "@InjectDataSource Object data_source;"
                , "}"
            ));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PROTOCOL, _PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format(
            "@InjectDataSource fields must be the same as the Presentation's Protocol, which is %s (%s)."
            , "test.Protocol", "test.NonProtocolDataSourcePresentation.data_source"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testPresentationFragmentDataSourceFailsIfNotProtocol() {
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("test.NonProtocolDataSourcePresentationFragment"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_FRAGMENT
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_FRAGMENT_IMPLEMENTATION
            , _PRESENTATION_FRAGMENT_IMPLEMENTATION_ANNOTATION
            ,
            "public class NonProtocolDataSourcePresentationFragment extends Fragment implements PresentationFragmentWithProtocolInterface {"
            , "@InjectDataSource Object data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(
            Arrays.asList(_OTHER_PROTOCOL, _PRESENTATION_FRAGMENT_INTERFACE, presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format(
            "@InjectDataSource fields must be the same as the Presentation Fragment's Protocol, "
                + "which is %s (%s)."
            , "test.OtherProtocol", "test.NonProtocolDataSourcePresentationFragment.data_source"))
        .in(presentation_fragment)
        .onLine(SEVEN);
  }

  @Test public void testPresentationDataSource() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithDataSource", Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithDataSource extends Activity implements PresentationWithProtocolInterface {"
            , "@InjectDataSource Protocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PROTOCOL, _PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .compilesWithoutError();
  }

  @Test public void testPresentationFragmentDataSource() {
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("test.PresentationFragmentWithDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_FRAGMENT
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_FRAGMENT_IMPLEMENTATION
            , _PRESENTATION_FRAGMENT_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationFragmentWithDataSource extends Fragment implements PresentationFragmentWithProtocolInterface {"
            , "@InjectDataSource OtherProtocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(
            Arrays.asList(_OTHER_PROTOCOL, _PRESENTATION_FRAGMENT_INTERFACE, presentation_fragment))
        .processedWith(prestigeProcessors())
        .compilesWithoutError();
  }

  @Test public void testInjectDataSourceFailsIfNonAnnotatedPresentation() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.NonAnnotatedPresentationWithDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_IMPORT_DATA_SOURCE
            ,
            "public class NonAnnotatedPresentationWithDataSource extends Activity implements PresentationWithProtocolInterface {"
            , "@InjectDataSource Protocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PROTOCOL, _PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format(
            "@InjectDataSource annotations must be specified in @PresentationImplementation or "
                + " @PresentationFragmentImplementation-annotated classes (%s)."
            , "test.NonAnnotatedPresentationWithDataSource"));
  }

  @Test public void testInjectDataSourceFailsIfNonAnnotatedPresentationFragment() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.NonAnnotatedPresentationFragmentWithDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_FRAGMENT
            , _IMPORT_IMPORT_DATA_SOURCE
            , "public class NonAnnotatedPresentationFragmentWithDataSource extends Fragment"
            , "implements PresentationFragmentWithProtocolInterface {"
            , "@InjectDataSource OtherProtocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_OTHER_PROTOCOL, _PRESENTATION_FRAGMENT_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format(
            "@InjectDataSource annotations must be specified in @PresentationImplementation or "
                + " @PresentationFragmentImplementation-annotated classes (%s)."
            , "test.NonAnnotatedPresentationFragmentWithDataSource"));
  }

  @Test public void testPresentationDataSourceFailsIfPrivate() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithPrivateDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithPrivateDataSource extends Activity implements PresentationWithProtocolInterface {"
            , "@InjectDataSource private Protocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PROTOCOL, _PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format(PRIVATE_OR_STATIC_INJECT_DATA_SOURCE_FIELDS_ERROR
                , "test.PresentationWithPrivateDataSource.data_source"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testPresentationFragmentDataSourceFailsIfPrivate() {
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("test.PresentationFragmentWithPrivateDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_FRAGMENT
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_FRAGMENT_IMPLEMENTATION
            , _PRESENTATION_FRAGMENT_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationFragmentWithPrivateDataSource extends Fragment implements PresentationFragmentWithProtocolInterface {"
            , "@InjectDataSource private OtherProtocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(
            Arrays.asList(_OTHER_PROTOCOL, _PRESENTATION_FRAGMENT_INTERFACE, presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format(PRIVATE_OR_STATIC_INJECT_DATA_SOURCE_FIELDS_ERROR
                , "test.PresentationFragmentWithPrivateDataSource.data_source"))
        .in(presentation_fragment)
        .onLine(SEVEN);
  }

  @Test public void testPresentationDataSourceFailsIfStatic() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithStaticDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithStaticDataSource extends Activity implements PresentationWithProtocolInterface {"
            , "@InjectDataSource static Protocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PROTOCOL, _PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format(PRIVATE_OR_STATIC_INJECT_DATA_SOURCE_FIELDS_ERROR
                , "test.PresentationWithStaticDataSource.data_source"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testPresentationFragmentDataSourceFailsIfStatic() {
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("test.PresentationFragmentWithStaticDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_FRAGMENT
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_FRAGMENT_IMPLEMENTATION
            , _PRESENTATION_FRAGMENT_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationFragmentWithStaticDataSource extends Fragment implements PresentationFragmentWithProtocolInterface {"
            , "@InjectDataSource static OtherProtocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(
            Arrays.asList(_OTHER_PROTOCOL, _PRESENTATION_FRAGMENT_INTERFACE, presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format(PRIVATE_OR_STATIC_INJECT_DATA_SOURCE_FIELDS_ERROR
                , "test.PresentationFragmentWithStaticDataSource.data_source"))
        .in(presentation_fragment)
        .onLine(SEVEN);
  }

  @Test public void testPresentationDataSourceFailsIfNoProtocolDefined() {
    final JavaFileObject presentation =
        JavaFileObjects.forSourceString("test.PresentationWithoutProtocolDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_ACTIVITY
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_IMPLEMENTATION
            , _PRESENTATION_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationWithoutProtocolDataSource extends Activity implements PresentationInterface {"
            , "@InjectDataSource Protocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(Arrays.asList(_PROTOCOL, _PRESENTATION_INTERFACE, presentation))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(String.format("@InjectDataSource may only be used with Presentations"
            + " that have a Protocol (%s)."
            , "test.PresentationWithoutProtocolDataSource"))
        .in(presentation)
        .onLine(SEVEN);
  }

  @Test public void testPresentationFragmentDataSourceFailsIfNotProtocolDefined() {
    final JavaFileObject presentation_fragment =
        JavaFileObjects.forSourceString("test.PresentationFragmentWithoutProtocolDataSource"
            , Joiner.on('\n').join(
            _PACKAGE_TEST
            , _IMPORT_FRAGMENT
            , _IMPORT_IMPORT_DATA_SOURCE
            , _IMPORT_PRESENTATION_FRAGMENT_IMPLEMENTATION
            , _PRESENTATION_FRAGMENT_IMPLEMENTATION_ANNOTATION
            ,
            "public class PresentationFragmentWithoutProtocolDataSource extends Fragment implements PresentationFragmentInterface {"
            , "@InjectDataSource OtherProtocol data_source;"
            , "}"
        ));

    ASSERT.about(javaSources())
        .that(
            Arrays.asList(_OTHER_PROTOCOL, _PRESENTATION_FRAGMENT_INTERFACE, presentation_fragment))
        .processedWith(prestigeProcessors())
        .failsToCompile()
        .withErrorContaining(
            String.format("@InjectDataSource may only be used with Presentation Fragments"
                + " that have a Protocol (%s)."
                , "test.PresentationFragmentWithoutProtocolDataSource"))
        .in(presentation_fragment)
        .onLine(SEVEN);
  }

  private static final JavaFileObject _PROTOCOL = JavaFileObjects.forResource("Protocol.java");
  private static final JavaFileObject _OTHER_PROTOCOL =
      JavaFileObjects.forResource("OtherProtocol.java");
  private static final JavaFileObject _PRESENTATION_INTERFACE =
      JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
  private static final JavaFileObject _PRESENTATION_FRAGMENT_INTERFACE =
      JavaFileObjects.forResource("PresentationFragmentWithProtocolInterface.java");
  private static final String _PACKAGE_TEST = "package test;";
  private static final String _IMPORT_ACTIVITY = "import android.app.Activity;";
  private static final String _IMPORT_IMPORT_DATA_SOURCE =
      "import com.imminentmeals.prestige.annotations.InjectDataSource;";
  private static final String _IMPORT_PRESENTATION_IMPLEMENTATION =
      "import com.imminentmeals.prestige.annotations.PresentationImplementation;";
  private static final String _PRESENTATION_IMPLEMENTATION_ANNOTATION =
      "@PresentationImplementation";
  private static final String _IMPORT_FRAGMENT = "import android.app.Fragment;";
  private static final String _IMPORT_PRESENTATION_FRAGMENT_IMPLEMENTATION =
      "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;";
  private static final String _PRESENTATION_FRAGMENT_IMPLEMENTATION_ANNOTATION =
      "@PresentationFragmentImplementation";
  private static final String PRIVATE_OR_STATIC_INJECT_DATA_SOURCE_FIELDS_ERROR =
      "@InjectDataSource fields must not be private or static (%s).";
}
