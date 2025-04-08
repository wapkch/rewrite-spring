/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.spring.framework;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateMethodArgumentNotValidExceptionErrorMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .typeValidationOptions(TypeValidation.none())
          .recipe(new MigrateMethodArgumentNotValidExceptionErrorMethod())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "spring-web-6.1.+", "spring-core", "spring-context"));
    }

    @DocumentExample
    @Test
    void migrateResourceHttpMessageWriterAddHeadersMethod() {
        rewriteRun(
          // language=java
          java(
            """
              import org.springframework.context.MessageSource;
              import java.util.Locale;
              import org.springframework.web.bind.MethodArgumentNotValidException;
              import java.util.List;
              import java.util.Map;
              import org.springframework.validation.BindException;
              import org.springframework.validation.ObjectError;

              class A {
                  public void handleValidationError(BindException bindException, MethodArgumentNotValidException methodArgumentNotValidException, MessageSource messageSource, Locale locale) {
                      List<ObjectError> errors = bindException.getAllErrors();
                      List<String> errorMessages = MethodArgumentNotValidException.errorsToStringList(errors);
                      List<String> errorMessages2 = MethodArgumentNotValidException.errorsToStringList(errors, null, Locale.CANADA);
                      Map<ObjectError, String> errorMessages = methodArgumentNotValidException.resolveErrorMessages(messageSource, locale);
                  }
              }
              """,
            """
              import org.springframework.context.MessageSource;
              import java.util.Locale;
              import org.springframework.web.bind.MethodArgumentNotValidException;
              import org.springframework.web.util.BindErrorUtils;

              import java.util.List;
              import java.util.Map;
              import org.springframework.validation.BindException;
              import org.springframework.validation.ObjectError;

              class A {
                  public void handleValidationError(BindException bindException, MethodArgumentNotValidException methodArgumentNotValidException, MessageSource messageSource, Locale locale) {
                      List<ObjectError> errors = bindException.getAllErrors();
                      List<String> errorMessages = BindErrorUtils.resolve(errors).values().stream().toList();
                      List<String> errorMessages2 = (null != null ?
                              BindErrorUtils.resolve(errors, null, Locale.CANADA).values().stream().toList() :
                              BindErrorUtils.resolve(errors).values().stream().toList());
                      Map<ObjectError, String> errorMessages = BindErrorUtils.resolve(methodArgumentNotValidException.getAllErrors(), messageSource, locale);
                  }
              }
              """
          )
        );
    }
}
