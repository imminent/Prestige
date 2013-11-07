package com.imminentmeals.prestige.codegen;

import java.util.Arrays;

import javax.annotation.processing.Processor;

/**
 * Test utilities.
 * @author Dandré
 */
/* package */final class ProcessorTestUtilities {

    /* package */static Iterable<? extends Processor> prestigeProcessors() {
        return Arrays.asList(
                new AnnotationProcessor()
        );
    }
}
