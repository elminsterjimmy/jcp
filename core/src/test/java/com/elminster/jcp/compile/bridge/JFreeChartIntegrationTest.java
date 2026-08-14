package com.elminster.jcp.compile.bridge;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.util.ClassConverter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test: JCP code drives JFreeChart to build bar and pie charts.
 *
 * <p>Exercises the full JCP↔Java type bridge:
 * <ul>
 *   <li>Eval mode: ClassConverter registers JFreeChart classes; JCP AST constructs a dataset,
 *       calls addValue / setValue on it, then creates a chart via ChartFactory static methods.</li>
 *   <li>Compile mode (bar chart): BytecodeGenerator + registerExternalClass;
 *       JFreeChart JAR loaded via an isolated URLClassLoader wired into MultiClassLoader via
 *       registerClassLoader — exactly the D2 scenario that was fixed.</li>
 * </ul>
 */
class JFreeChartIntegrationTest {

    private static final AtomicInteger counter = new AtomicInteger();

    private String genName(String base) {
        return base + "_" + counter.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    // Eval mode
    // -------------------------------------------------------------------------

    @Nested
    class EvalMode {

        private RootEvalContext ctx;

        @BeforeEach
        void setUp() {
            ctx = new RootEvalContext();
            ClassConverter.registerClass(DefaultCategoryDataset.class, ctx, "user");
            ClassConverter.registerClass(DefaultPieDataset.class,      ctx, "user");
            ClassConverter.registerClass(ChartFactory.class,           ctx, "user");
            ClassConverter.registerClass(JFreeChart.class,             ctx, "user");
        }

        /**
         * Eval mode — bar chart.
         *
         * <pre>
         * dataset = DefaultCategoryDataset.new()
         * dataset.addValue(42.0, "Series1", "Category1")
         * dataset.addValue(17.0, "Series1", "Category2")
         * chart   = ChartFactory.createBarChart("Sales", "Category", "Value", dataset)
         * title   = chart.getTitle()
         * result  = title.getText()   // → "Sales"
         * </pre>
         */
        @Test
        void barChart_evalMode_titleMatchesInput() throws Exception {
            ClassConverter.registerClass(
                    Class.forName("org.jfree.chart.title.TextTitle"), ctx, "user");

            Block program = new BlockImpl();

            // dataset = DefaultCategoryDataset.new()
            program.addStatement(new VariableDeclarationImpl("dataset", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("user::DefaultCategoryDataset.new"))));

            // dataset.addValue(42.0, "Series1", "Category1")
            program.addStatement(new ExpressionStatement(
                    new FunctionCallExpression(Identifier.fromName("user::DefaultCategoryDataset.addValue"),
                            new VariableExpression(Identifier.fromName("dataset")),
                            LiteralExpression.of(42.0),
                            LiteralExpression.of("Series1"),
                            LiteralExpression.of("Category1"))));

            // dataset.addValue(17.0, "Series1", "Category2")
            program.addStatement(new ExpressionStatement(
                    new FunctionCallExpression(Identifier.fromName("user::DefaultCategoryDataset.addValue"),
                            new VariableExpression(Identifier.fromName("dataset")),
                            LiteralExpression.of(17.0),
                            LiteralExpression.of("Series1"),
                            LiteralExpression.of("Category2"))));

            // chart = ChartFactory.createBarChart("Sales", "Category", "Value", dataset)
            program.addStatement(new VariableDeclarationImpl("chart", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("user::ChartFactory.createBarChart"),
                            LiteralExpression.of("Sales"),
                            LiteralExpression.of("Category"),
                            LiteralExpression.of("Value"),
                            new VariableExpression(Identifier.fromName("dataset")))));

            // title = chart.getTitle()
            program.addStatement(new VariableDeclarationImpl("title", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("user::JFreeChart.getTitle"),
                            new VariableExpression(Identifier.fromName("chart")))));

            // result = title.getText()
            program.addStatement(new VariableDeclarationImpl("result", SystemDataType.STRING,
                    new FunctionCallExpression(Identifier.fromName("user::TextTitle.getText"),
                            new VariableExpression(Identifier.fromName("title")))));

            new EvalVisitor(ctx).visit(program);

            Object result = ctx.getVariable("result").get();
            assertEquals("Sales", result, "Bar chart title must match");
        }

        /**
         * Eval mode — pie chart.
         *
         * <pre>
         * dataset = DefaultPieDataset.new()
         * dataset.setValue("Slice A", 60.0)
         * dataset.setValue("Slice B", 40.0)
         * chart   = ChartFactory.createPieChart("Market Share", dataset)
         * title   = chart.getTitle()
         * result  = title.getText()   // → "Market Share"
         * </pre>
         */
        @Test
        void pieChart_evalMode_titleMatchesInput() throws Exception {
            ClassConverter.registerClass(
                    Class.forName("org.jfree.chart.title.TextTitle"), ctx, "user");

            Block program = new BlockImpl();

            // dataset = DefaultPieDataset.new()
            program.addStatement(new VariableDeclarationImpl("dataset", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("user::DefaultPieDataset.new"))));

            // dataset.setValue("Slice A", 60.0)
            program.addStatement(new ExpressionStatement(
                    new FunctionCallExpression(Identifier.fromName("user::DefaultPieDataset.setValue"),
                            new VariableExpression(Identifier.fromName("dataset")),
                            LiteralExpression.of("Slice A"),
                            LiteralExpression.of(60.0))));

            // dataset.setValue("Slice B", 40.0)
            program.addStatement(new ExpressionStatement(
                    new FunctionCallExpression(Identifier.fromName("user::DefaultPieDataset.setValue"),
                            new VariableExpression(Identifier.fromName("dataset")),
                            LiteralExpression.of("Slice B"),
                            LiteralExpression.of(40.0))));

            // chart = ChartFactory.createPieChart("Market Share", dataset)
            program.addStatement(new VariableDeclarationImpl("chart", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("user::ChartFactory.createPieChart"),
                            LiteralExpression.of("Market Share"),
                            new VariableExpression(Identifier.fromName("dataset")))));

            // title = chart.getTitle()
            program.addStatement(new VariableDeclarationImpl("title", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("user::JFreeChart.getTitle"),
                            new VariableExpression(Identifier.fromName("chart")))));

            // result = title.getText()
            program.addStatement(new VariableDeclarationImpl("result", SystemDataType.STRING,
                    new FunctionCallExpression(Identifier.fromName("user::TextTitle.getText"),
                            new VariableExpression(Identifier.fromName("title")))));

            new EvalVisitor(ctx).visit(program);

            Object result = ctx.getVariable("result").get();
            assertEquals("Market Share", result, "Pie chart title must match");
        }
    }

    // -------------------------------------------------------------------------
    // Compile mode — bar chart via isolated URLClassLoader (D2 scenario)
    // -------------------------------------------------------------------------

    @Nested
    class CompileMode {

        /**
         * Compile mode — bar chart built entirely from JCP bytecode.
         *
         * <p>JFreeChart is loaded via a URLClassLoader whose parent is the platform
         * classloader only (not the app classloader), reproducing the D2 isolation
         * scenario. The generated class references JFreeChart types; they resolve only
         * because the URLClassLoader is registered via {@code registerClassLoader}.
         *
         * <p>The compiled program:
         * <pre>
         * dataset = DefaultCategoryDataset.new()
         * dataset.addValue(42.0, "Series1", "Cat1")
         * return ChartFactory.createBarChart("Revenue", "Month", "USD", dataset).getTitle().getText()
         * </pre>
         * Expected: "Revenue"
         */
        @Test
        void barChart_compileMode_isolatedLoader_titleMatchesInput() throws Exception {
            // Locate JFreeChart jar in local Maven repo
            String m2 = System.getProperty("user.home") + "/.m2/repository";
            URL jfreecharUrl = Paths.get(m2,
                    "org/jfree/jfreechart/1.5.5/jfreechart-1.5.5.jar").toUri().toURL();

            // Isolated loader — platform parent only, not on app classpath
            URLClassLoader isolatedLoader = new URLClassLoader(
                    new URL[]{jfreecharUrl},
                    ClassLoader.getPlatformClassLoader());

            Class<?> datasetClass  = isolatedLoader.loadClass("org.jfree.data.category.DefaultCategoryDataset");
            Class<?> factoryClass  = isolatedLoader.loadClass("org.jfree.chart.ChartFactory");
            Class<?> chartClass    = isolatedLoader.loadClass("org.jfree.chart.JFreeChart");
            Class<?> textTitleClass = isolatedLoader.loadClass("org.jfree.chart.title.TextTitle");

            String className = genName("JFreeBarChart");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            generator.registerExternalClass(datasetClass);
            generator.registerExternalClass(factoryClass);
            generator.registerExternalClass(chartClass);
            generator.registerExternalClass(textTitleClass);

            // Build AST:
            // dataset = DefaultCategoryDataset.new()
            // dataset.addValue(42.0, "Series1", "Cat1")
            // return ChartFactory.createBarChart("Revenue","Month","USD",dataset)
            //            .getTitle().getText()
            //
            // Since compile mode only supports compileWithReturn (single expression),
            // we put dataset construction + addValue in the preamble Block and return
            // the title string from a chained StaticMethodCallExpression.
            //
            // The chained call is represented with intermediate variable declarations
            // followed by a final StaticMethodCallExpression returning STRING.

            Block program = new BlockImpl();

            // dataset = DefaultCategoryDataset.new()
            program.addStatement(new VariableDeclarationImpl("dataset", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("DefaultCategoryDataset.new"))));

            // dataset.addValue(42.0, "Series1", "Cat1")  — instance method call
            program.addStatement(new ExpressionStatement(
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("dataset")),
                            "addValue",
                            LiteralExpression.of(42.0),
                            LiteralExpression.of("Series1"),
                            LiteralExpression.of("Cat1"))));

            // chart = ChartFactory.createBarChart("Revenue", "Month", "USD", dataset)
            program.addStatement(new VariableDeclarationImpl("chart", SystemDataType.ANY,
                    new StaticMethodCallExpression("ChartFactory", "createBarChart",
                            LiteralExpression.of("Revenue"),
                            LiteralExpression.of("Month"),
                            LiteralExpression.of("USD"),
                            new VariableExpression(Identifier.fromName("dataset")))));

            // title = chart.getTitle()  — instance method call
            program.addStatement(new VariableDeclarationImpl("title", SystemDataType.ANY,
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("chart")),
                            "getTitle")));

            // return title.getText()  → STRING
            MethodCallExpression getTextExpr = new MethodCallExpression(
                    new VariableExpression(Identifier.fromName("title")),
                    "getText");

            byte[] bytecode = generator.compileWithReturn(program, getTextExpr, SystemDataType.STRING);

            // Load with registerClassLoader so JFreeChart types resolve
            MultiClassLoader loader = new MultiClassLoader();
            loader.registerClassLoader(isolatedLoader);
            loader.defineClass(className, bytecode);

            Class<?> clazz = loader.loadClass(className);
            Object result = clazz.getMethod("evaluate").invoke(null);
            assertEquals("Revenue", result,
                    "Compile-mode bar chart title must match");

            isolatedLoader.close();
        }
    }
}
