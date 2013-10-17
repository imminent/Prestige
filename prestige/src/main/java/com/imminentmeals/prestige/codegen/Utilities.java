/*
 * Copyright (C) 2013 Google, Inc.
 * Copyright (C) 2013 Square, Inc.
 * Copyright (C) 2013 ImminentMeals
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.imminentmeals.prestige.codegen;

import android.annotation.TargetApi;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;

import static android.os.Build.VERSION_CODES.GINGERBREAD;

/* package */final class Utilities {

    /* package */static PackageElement getPackage(Element type) {
        while (type.getKind() != ElementKind.PACKAGE) type = type.getEnclosingElement();

        return (PackageElement) type;
    }

    /**
     * Returns a string for the raw type of {@code type}. Primitive types are always boxed.
     */
    /* package */static String rawTypeToString(TypeMirror type, char inner_class_separator) {
        if (!(type instanceof DeclaredType)) {
            throw new IllegalArgumentException("Unexpected type: " + type);
        }
        final StringBuilder result = new StringBuilder();
        final DeclaredType declared_type = (DeclaredType) type;
        rawTypeToString(result, (TypeElement) declared_type.asElement(), inner_class_separator);
        return result.toString();
    }


    /**
     * Returns the annotation on {@code element} formatted as a Map. This returns
     * a Map rather than an instance of the annotation interface to work-around
     * the fact that Class and Class[] fields won't work at code generation time.
     * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5089128
     */
    /* package */static Map<String, Object> getAnnotation(Class<?> annotation_type, Element element) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (!rawTypeToString(annotation.getAnnotationType(), '$')
                    .equals(annotation_type.getName())) {
                continue;
            }

            final Map<String, Object> result = new LinkedHashMap<String, Object>();
            for (Method method : annotation_type.getMethods()) {
                result.put(method.getName(), method.getDefaultValue());
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                    : annotation.getElementValues().entrySet()) {
                final String name = entry.getKey().getSimpleName().toString();
                final Object value = entry.getValue().accept(VALUE_EXTRACTOR, null);
                final Object default_value = result.get(name);
                if (!lenientIsInstance(default_value.getClass(), value)) {
                    throw new IllegalStateException(String.format(
                            "Value of %s.%s is a %s but expected a %s%n    value: %s"
                          , annotation_type, name, value.getClass().getName(), default_value.getClass().getName()
                          , value instanceof Object[] ? Arrays.toString((Object[]) value) : value));
                }
                result.put(name, value);
            }
            return result;
        }

        // Annotation not found
        return null;
    }

    @TargetApi(GINGERBREAD)
    /* package */static void rawTypeToString(StringBuilder result, TypeElement type
                                           , char inner_class_separator) {
        final String package_name = getPackage(type).getQualifiedName().toString();
        final String qualified_name = type.getQualifiedName().toString();
        if (package_name.isEmpty()) {
            result.append(qualified_name.replace('.', inner_class_separator));
        } else {
            result.append(package_name);
            result.append('.');
            result.append(
                    qualified_name.substring(package_name.length() + 1).replace('.', inner_class_separator));
        }
    }

    /**
     * Returns true if {@code value} can be assigned to {@code expected_class}.
     * Like {@link Class#isInstance} but more lenient for {@code Class<?>} values.
     */
    private static boolean lenientIsInstance(Class<?> expected_class, Object value) {
        if (expected_class.isArray()) {
            final Class<?> component_type = expected_class.getComponentType();
            if (!(value instanceof Object[])) return false;
            for (Object element : (Object[]) value) {
                if (!lenientIsInstance(component_type, element)) return false;
            }
            return true;
        } else if (expected_class == Class.class) {
            return value instanceof TypeMirror;
        } else {
            return expected_class == value.getClass();
        }
    }

    private static final AnnotationValueVisitor<Object, Void> VALUE_EXTRACTOR
            = new SimpleAnnotationValueVisitor6<Object, Void>() {

        protected Object defaultAction(Object object, Void _) {
            return object;
        }

        public Object visitArray(List<? extends AnnotationValue> values, Void _) {
            Object[] result = new Object[values.size()];
            for (int i = 0; i < values.size(); i++) result[i] = values.get(i).accept(this, null);
            return result;
        }
    };

/* Private Constructor */
    private Utilities() { }
}
