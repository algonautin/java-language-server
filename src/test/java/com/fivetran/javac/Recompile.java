package com.fivetran.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import org.junit.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class Recompile extends Fixtures {
    @Test
    public void compileTwice() {
        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
        GetResourceFileObject file = new GetResourceFileObject("/CompileTwice.java");
        JavacHolder compiler = new JavacHolder(Collections.emptyList(),
                                               Collections.singletonList(Paths.get("src/test/resources")),
                                               Paths.get("out"));
        List<String> visits = new ArrayList<>();
        compiler.afterAnalyze(new GetClass(compiler.context, visits));
        compiler.onError(errors);
        compiler.compile(compiler.parse(file));

        assertThat(errors.getDiagnostics(), empty());
        assertThat(visits, contains("CompileTwice"));

        // Compile again
        compiler.onError(errors);
        compiler.compile(compiler.parse(file));

        assertThat(errors.getDiagnostics(), empty());
        assertThat(visits, contains("CompileTwice", "CompileTwice"));
    }

    @Test
    public void fixParseError() {
        Path path = Paths.get("FixParseError.java");
        StringFileObject bad = new StringFileObject("public class FixParseError { public String foo() { return \"foo\"; }", path);
        StringFileObject good = new StringFileObject("public class FixParseError { public String foo() { return \"foo\"; } }", path);
        JavacHolder compiler = new JavacHolder(Collections.emptyList(),
                                               Collections.singletonList(Paths.get("src/test/resources")),
                                               Paths.get("out"));
        DiagnosticCollector<JavaFileObject> badErrors = new DiagnosticCollector<>();

        compiler.onError(badErrors);
        compiler.parse(bad);

        DiagnosticCollector<JavaFileObject> goodErrors = new DiagnosticCollector<>();

        assertThat(badErrors.getDiagnostics(), not(empty()));

        // Parse again
        List<String> parsedClassNames = new ArrayList<>();

        compiler.afterParse(new GetClass(compiler.context, parsedClassNames));
        compiler.onError(goodErrors);
        compiler.parse(good);

        assertThat(goodErrors.getDiagnostics(), empty());
        assertThat(parsedClassNames, contains("FixParseError"));
    }

    @Test
    public void fixTypeError() {
        Path path = Paths.get("FixTypeError.java");
        StringFileObject bad = new StringFileObject("public class FixTypeError { public String foo() { return 1; } }", path);
        StringFileObject good = new StringFileObject("public class FixTypeError { public String foo() { return \"foo\"; } }", path);
        JavacHolder compiler = new JavacHolder(Collections.emptyList(),
                                               Collections.singletonList(Paths.get("src/test/resources")),
                                               Paths.get("out"));
        DiagnosticCollector<JavaFileObject> badErrors = new DiagnosticCollector<>();

        compiler.onError(badErrors);
        compiler.compile(compiler.parse(bad));

        DiagnosticCollector<JavaFileObject> goodErrors = new DiagnosticCollector<>();

        assertThat(badErrors.getDiagnostics(), not(empty()));

        // Parse again
        List<String> parsedClassNames = new ArrayList<>();

        compiler.afterAnalyze(new GetClass(compiler.context, parsedClassNames));
        compiler.onError(goodErrors);
        compiler.compile(compiler.parse(good));

        assertThat(goodErrors.getDiagnostics(), empty());
        assertThat(parsedClassNames, contains("FixTypeError"));
    }

    private static class GetClass extends BaseScanner {
        private final List<String> visits;

        public GetClass(Context context, List<String> visits) {
            super(context);

            this.visits = visits;
        }

        @Override
        public void visitClassDef(JCTree.JCClassDecl tree) {
            super.visitClassDef(tree);

            visits.add(tree.getSimpleName().toString());
        }
    }
}
