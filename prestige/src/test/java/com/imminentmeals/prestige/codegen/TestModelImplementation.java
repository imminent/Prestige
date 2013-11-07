package com.imminentmeals.prestige.codegen;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.SIX;
import static com.imminentmeals.prestige.codegen.ProcessorTestUtilities.prestigeProcessors;
import static org.truth0.Truth.ASSERT;

public class TestModelImplementation {

    @Test
    public void sameScopeModelImplementationsFailsIfInDifferentPackage() {
        final JavaFileObject model_interface = JavaFileObjects.forResource("ModelInterface.java");
        final JavaFileObject model = JavaFileObjects.forResource("TestModel.java");
        final JavaFileObject other_model_interface = JavaFileObjects.forResource("DifferentPackageModelInterface.java");
        final JavaFileObject other_model = JavaFileObjects.forResource("DifferentPackageModel.java");

        ASSERT.about(javaSources())
              .that(Arrays.asList(
                      model_interface
                    , model
                    , other_model_interface
                    , other_model))
              .processedWith(prestigeProcessors())
              .failsToCompile()
              .withErrorContaining(String.format(
                        "All @ModelImplementation(\"%s\") must be defined in the same package (%s)."
                      , TEST, "different.DifferentPackageModel"))
              .in(other_model_interface)
              .onLine(SIX);
    }

    @Test
    public void sameScopeControllerImplementationsInSamePackage() {
        final JavaFileObject model_interface = JavaFileObjects.forResource("ModelInterface.java");
        final JavaFileObject model = JavaFileObjects.forResource("TestModel.java");
        final JavaFileObject other_model_interface = JavaFileObjects.forResource("OtherModelInterface.java");
        final JavaFileObject other_model = JavaFileObjects.forResource("OtherModel.java");

        ASSERT.about(javaSources())
                .that(Arrays.asList(
                        model_interface
                        , model
                        , other_model_interface
                        , other_model))
                .processedWith(prestigeProcessors())
                .compilesWithoutError();
    }
}
