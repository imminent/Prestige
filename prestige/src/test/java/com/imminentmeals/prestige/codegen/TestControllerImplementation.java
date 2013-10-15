package com.imminentmeals.prestige.codegen;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

@Ignore("Bugs to avoid")
@RunWith(JUnit4.class)
public class TestControllerImplementation {

    @Test
    public void sameScopeControllerImplementationsFailsIfInSamePackage() {
        final JavaFileObject presentation = JavaFileObjects.forResource("PresentationInterface.java");
        final JavaFileObject controller_interface = JavaFileObjects.forResource("ControllerInterface.java");
        final JavaFileObject controller = JavaFileObjects.forResource("TestController.java");
        final JavaFileObject other_presentation = JavaFileObjects.forResource("DifferentPackagePresentationInterface.java");
        final JavaFileObject other_controller_interface = JavaFileObjects.forResource("DifferentPackageControllerInterface.java");
        final JavaFileObject other_controller = JavaFileObjects.forResource("DifferentPackageController.java");

        ASSERT.about(javaSources())
              .that(Arrays.asList(presentation
                                , controller_interface
                                , controller
                                , other_presentation
                                , other_controller_interface
                                , other_controller))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format(
                      "All @ControllerImplementation(\"%s\") must be defined in the same package (%s)."
                    , TEST, "TestController"))
              .in(other_controller).onLine(7);
    }
}
