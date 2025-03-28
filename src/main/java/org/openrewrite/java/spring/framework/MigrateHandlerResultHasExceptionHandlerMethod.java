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

import java.util.List;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

public class MigrateHandlerResultHasExceptionHandlerMethod extends Recipe {

    private static final String HandlerResult = "org.springframework.web.reactive.HandlerResult";

    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher(HandlerResult + " hasExceptionHandler()");

    @Override
    public String getDisplayName() {
        return "Migrate `org.springframework.web.reactive.HandlerResult.setExceptionHandler` method";
    }

    @Override
    public String getDescription() {
        return "`org.springframework.web.reactive.HandlerResult.setExceptionHandler(Function<Throwable, Mono<HandlerResult>>)` was deprecated, in favor of `setExceptionHandler(DispatchExceptionHandler)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(HandlerResult, false), new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                List<Statement> statements = method.getBody().getStatements();
                for (Statement statement : statements) {
                    if (statement instanceof J.MethodInvocation) {
                        J.MethodInvocation methodInvocation = (J.MethodInvocation) statement;
                        if (METHOD_MATCHER.matches(methodInvocation)) {
                            // Replace the method call with the new method call
                            JavaTemplate template = JavaTemplate.builder("private boolean hasExceptionHandler(HandlerResult result) {\n" +
                                    "        return result.getExceptionHandler() != null;\n" +
                                    "    }")
                                .build();
                            return template.apply(getCursor(), method.getCoordinates().after());
                        }
                    }
                }

                return super.visitMethodDeclaration(method, executionContext);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (METHOD_MATCHER.matches(m)) {
                    Cursor classDeclarationCursor = getCursor().dropParentUntil(it -> it instanceof J.ClassDeclaration || it == Cursor.ROOT_VALUE);
                    JavaTemplate.builder("private boolean hasExceptionHandler(HandlerResult result) {\n" +
                            "        return result.getExceptionHandler() != null;\n" +
                            "    }")
                        .build()
                        .apply(classDeclarationCursor, );

                    return JavaTemplate.builder("#{any()}.getExceptionHandler() != null")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                return m;
            }
        });
    }

}
