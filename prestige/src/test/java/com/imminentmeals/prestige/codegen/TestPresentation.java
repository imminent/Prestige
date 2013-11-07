package com.imminentmeals.prestige.codegen;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.FOUR;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SIX;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.THREE;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestPresentation {

    @Test
    public void testPresentation() {
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forResource("TestPresentation.java");

        ASSERT.about(javaSources())
              .that(Arrays.asList(presentation_interface, presentation))
              .processedWith(prestigeProcessors())
              .compilesWithoutError();
    }

    @Test
    public void testPresentationFailsIfNotInterface() {
        final JavaFileObject presentation = JavaFileObjects.forSourceString("NotInterfacePresentation", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Presentation;"
              , "@Presentation"
              , "public class NotInterfacePresentation { }"));

        ASSERT.about(javaSource())
              .that(presentation)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation annotation may only be specified on interfaces (%s)."
                                               , "NotInterfacePresentation"))
              .in(presentation)
              .onLine(THREE);
    }

    @Test
    public void testPresentationFailsIfNotPublic() {
        JavaFileObject presentation = JavaFileObjects.forSourceString("PackageProtectedPresentation", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Presentation;"
              , "@Presentation"
              , "interface PackageProtectedPresentation { }"));

        ASSERT.about(javaSource())
              .that(presentation)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation interface must be public (%s)."
                                               , "PackageProtectedPresentation"))
              .in(presentation)
              .onLine(THREE);

        presentation = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Presentation;"
              , "public class Test {"
              , "@Presentation"
              , "private interface PrivatePresentation { }"
              , "}"));

        ASSERT.about(javaSource())
              .that(presentation)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation interface must be public (%s)."
                                               , "Test.PrivatePresentation"))
              .in(presentation)
              .onLine(4);

        presentation = JavaFileObjects.forSourceString("Test", Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Presentation;"
              , "public class Test {"
              , "@Presentation"
              , "protected interface ProtectedPresentation { }"
              , "}"));

        ASSERT.about(javaSource())
              .that(presentation)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation interface must be public (%s)."
                                               , "Test.ProtectedPresentation"))
              .in(presentation)
              .onLine(FOUR);
    }

    @Test
    public void testProtocolFailsIfNotInterface() {
        final JavaFileObject protocol = JavaFileObjects.forSourceString("NotInterfaceProtocol"
                , "public class NotInterfaceProtocol { }");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("PresentationWithProtocolInterface_broken"
                                                                          , Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Presentation;"
              , "@Presentation(protocol = NotInterfaceProtocol.class)"
              , "public interface PresentationWithProtocolInterface_broken { }"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(protocol, presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation Protocol must be an interface (%s)."
                                               , "NotInterfaceProtocol"));
    }

    @Test
    public void testProtocolFailsIfNotPublic() {
        final String source_format = Joiner.on('\n').join(
                "import com.imminentmeals.prestige.annotations.Presentation;"
              , "public class Test {"
              , "%1sinterface %2$s { }"
              , "@Presentation(protocol = %2$s.class)"
              , "public interface PresentationWithProtocolInterface_broken { }"
              , "}");
        String protocol_name = "PackageProtectedProtocol";
        JavaFileObject source = JavaFileObjects.forSourceString("Test", String.format(source_format, "", protocol_name));

        ASSERT.about(javaSource())
              .that(source)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation Protocol must be public (%s)."
                                               , "Test." + protocol_name));

        protocol_name = "PrivateProtocol";
        source = JavaFileObjects.forSourceString("Test", String.format(source_format, "", protocol_name));

        ASSERT.about(javaSource())
              .that(source)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation Protocol must be public (%s)."
                                               , "Test." + protocol_name));


        protocol_name = "ProtectedProtocol";
        source = JavaFileObjects.forSourceString("Test", String.format(source_format, "", protocol_name));

        ASSERT.about(javaSource())
              .that(source)
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation Protocol must be public (%s)."
                                               , "Test." + protocol_name));
    }

    @Test
    public void testProtocolFailsIfNotExtendingPresentationFragmentProtocols() {
        final JavaFileObject presentation_fragment_protocol = JavaFileObjects.forResource("OtherProtocol.java");
        final JavaFileObject presentation_fragment_interface = JavaFileObjects.forResource("PresentationFragmentWithProtocolInterface.java");
        final JavaFileObject protocol = JavaFileObjects.forResource("Protocol.java");
        final JavaFileObject presentation_interface = JavaFileObjects.forResource("PresentationWithProtocolInterface.java");
        final JavaFileObject presentation = JavaFileObjects.forSourceString("test.PresentationWithPresentationFragment_broken"
                                                                          , Joiner.on('\n').join(
                "package test;"
              , "import android.app.Activity;"
              , "import com.imminentmeals.prestige.annotations.PresentationImplementation;"
              , "import com.imminentmeals.prestige.annotations.InjectPresentationFragment;"
              , "@PresentationImplementation"
              , "public class PresentationWithPresentationFragment_broken extends Activity implements PresentationWithProtocolInterface {"
              , "@InjectPresentationFragment(manual = true) PresentationFragmentWithProtocolInterface presentation_fragment;"
              , "}"));

        ASSERT.about(javaSources())
              .that(Arrays.asList(
                      presentation_fragment_protocol
                    , presentation_fragment_interface
                    , protocol
                    , presentation_interface
                    , presentation))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format("@Presentation Protocol must extend %s from Presentation Fragment %s (%s)."
                                               , "test.OtherProtocol"
                                               , "test.PresentationFragmentWithProtocolInterface"
                                               , "test.PresentationWithPresentationFragment_broken.presentation_fragment"))
              .in(presentation)
              .onLine(SIX);
    }
}
