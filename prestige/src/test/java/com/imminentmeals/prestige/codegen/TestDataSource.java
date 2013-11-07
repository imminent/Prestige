package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SEVEN;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestDataSource {

    @Test
    public void testPresentationDataSourceFailsIfNotProtocol() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.NonProtocolDataSourcePresentation", Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "@PresentationImplementation"
              , "public class NonProtocolDataSourcePresentation extends Activity implements PresentationWithProtocolInterface {"
              , "@InjectDataSource Object data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource fields must be the same as the Presentation's Protocol, which is %s (%s)."
                      , "test.Protocol", "test.NonProtocolDataSourcePresentation.data_source"))
              .in(presentation)
              .onLine(SEVEN);
    }

    @Test
    public void testPresentationFragmentDataSourceFailsIfNotProtocol() {
        final JavaFileObject protocol = JavaFileObjects.forResource("OtherProtocol.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentWithProtocolInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("test.NonProtocolDataSourcePresentationFragment"
                                                                                   , Joiner.on('\n').join(
                "package test;"
                , "import android.app.Fragment;"
                , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
                , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
                , "@PresentationFragmentImplementation"
                , "public class NonProtocolDataSourcePresentationFragment extends Fragment implements PresentationFragmentWithProtocolInterface {"
                , "@InjectDataSource Object data_source;"
                , "}"
        ));

        ASSERT.about(javaSources())
                .that(Arrays.asList(protocol, presentation_fragment_interface, presentation_fragment))
                .processedWith(prestigeProcessors())
                .failsToCompile()
                .withErrorContaining(String.format("@InjectDataSource fields must be the same as the Presentation Fragment's Protocol, "
                                                 + "which is %s (%s)."
                                                 , "test.OtherProtocol", "test.NonProtocolDataSourcePresentationFragment.data_source"))
                .in(presentation_fragment)
                .onLine(SEVEN);
    }

    @Test
    public void testPresentationDataSource() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithDataSource", Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "@PresentationImplementation"
              , "public class PresentationWithDataSource extends Activity implements PresentationWithProtocolInterface {"
              , "@InjectDataSource Protocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();
    }

    @Test
    public void testPresentationFragmentDataSource() {
        final JavaFileObject protocol = JavaFileObjects.forResource("OtherProtocol.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentWithProtocolInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("test.PresentationFragmentWithDataSource"
                                                                                   , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
              , "@PresentationFragmentImplementation"
              , "public class PresentationFragmentWithDataSource extends Fragment implements PresentationFragmentWithProtocolInterface {"
              , "@InjectDataSource OtherProtocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_fragment_interface, presentation_fragment))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();
    }

    @Test
    public void testInjectDataSourceFailsIfNonAnnotatedPresentation() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.NonAnnotatedPresentationWithDataSource"
                                                                          , Joiner.on('\n').join(
                "package test;"
               , "import android.app.Activity;"
               , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
               , "public class NonAnnotatedPresentationWithDataSource extends Activity implements PresentationWithProtocolInterface {"
               , "@InjectDataSource Protocol data_source;"
               , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource annotations must be specified in @PresentationImplementation or "
                      + " @PresentationFragmentImplementation-annotated classes (%s)."
                      , "test.NonAnnotatedPresentationWithDataSource"));
    }

    @Test
    public void testInjectDataSourceFailsIfNonAnnotatedPresentationFragment() {
        final JavaFileObject protocol = JavaFileObjects.forResource("OtherProtocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationFragmentWithProtocolInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.NonAnnotatedPresentationFragmentWithDataSource"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "public class NonAnnotatedPresentationFragmentWithDataSource extends Fragment"
              , "implements PresentationFragmentWithProtocolInterface {"
              , "@InjectDataSource OtherProtocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource annotations must be specified in @PresentationImplementation or "
                      + " @PresentationFragmentImplementation-annotated classes (%s)."
                      , "test.NonAnnotatedPresentationFragmentWithDataSource"));
    }

    @Test
    public void testPresentationDataSourceFailsIfPrivate() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithPrivateDataSource"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "@PresentationImplementation"
              , "public class PresentationWithPrivateDataSource extends Activity implements PresentationWithProtocolInterface {"
              , "@InjectDataSource private Protocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource fields must not be private or static (%s)."
                                               , "test.PresentationWithPrivateDataSource.data_source"))
              .in(presentation)
              .onLine(7);
    }

    @Test
    public void testPresentationFragmentDataSourceFailsIfPrivate() {
        final JavaFileObject protocol = JavaFileObjects.forResource("OtherProtocol.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentWithProtocolInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("test.PresentationFragmentWithPrivateDataSource"
                                                                                   , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
              , "@PresentationFragmentImplementation"
              , "public class PresentationFragmentWithPrivateDataSource extends Fragment implements PresentationFragmentWithProtocolInterface {"
              , "@InjectDataSource private OtherProtocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
                .that(Arrays.asList(protocol, presentation_fragment_interface, presentation_fragment))
                .processedWith(prestigeProcessors())
                .failsToCompile()
                .withErrorContaining(String.format("@InjectDataSource fields must not be private or static (%s)."
                        , "test.PresentationFragmentWithPrivateDataSource.data_source"))
                .in(presentation_fragment)
                .onLine(SEVEN);
    }

    @Test
    public void testPresentationDataSourceFailsIfStatic() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithStaticDataSource"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "@PresentationImplementation"
              , "public class PresentationWithStaticDataSource extends Activity implements PresentationWithProtocolInterface {"
              , "@InjectDataSource static Protocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource fields must not be private or static (%s)."
                      , "test.PresentationWithStaticDataSource.data_source"))
              .in(presentation)
              .onLine(SEVEN);
    }

    @Test
    public void testPresentationFragmentDataSourceFailsIfStatic() {
        final JavaFileObject protocol = JavaFileObjects.forResource("OtherProtocol.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentWithProtocolInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("test.PresentationFragmentWithStaticDataSource"
                                                                                   , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
              , "@PresentationFragmentImplementation"
              , "public class PresentationFragmentWithStaticDataSource extends Fragment implements PresentationFragmentWithProtocolInterface {"
              , "@InjectDataSource static OtherProtocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_fragment_interface, presentation_fragment))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource fields must not be private or static (%s)."
                      , "test.PresentationFragmentWithStaticDataSource.data_source"))
              .in(presentation_fragment)
              .onLine(SEVEN);
    }

    @Test
    public void testPresentationDataSourceFailsIfNoProtocolDefined() {
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithoutProtocolDataSource"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "@PresentationImplementation"
              , "public class PresentationWithoutProtocolDataSource extends Activity implements PresentationInterface {"
              , "@InjectDataSource Protocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource may only be used with Presentations"
                                               + " that have a Protocol (%s)."
                                               , "test.PresentationWithoutProtocolDataSource"))
              .in(presentation)
              .onLine(SEVEN);
    }

    @Test
    public void testPresentationFragmentDataSourceFailsIfNotProtocolDefined() {
        final JavaFileObject protocol = JavaFileObjects.forResource("OtherProtocol.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentInterface.java");
        final JavaFileObject presentation_fragment = JavaFileObjects.forSourceString("test.PresentationFragmentWithoutProtocolDataSource"
                                                                                   , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Fragment;"
              , "import com.imminentmeals.prestige.annotations.InjectDataSource;"
              , "import com.imminentmeals.prestige.annotations.PresentationFragmentImplementation;"
              , "@PresentationFragmentImplementation"
              , "public class PresentationFragmentWithoutProtocolDataSource extends Fragment implements PresentationFragmentInterface {"
              , "@InjectDataSource OtherProtocol data_source;"
              , "}"
        ));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation_fragment_interface, presentation_fragment))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@InjectDataSource may only be used with Presentation Fragments"
                                               + " that have a Protocol (%s)."
                                               , "test.PresentationFragmentWithoutProtocolDataSource"))
              .in(presentation_fragment)
              .onLine(SEVEN);
    }
}
