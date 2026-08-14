package com.elminster.jcp.compile.bridge;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.util.ClassConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end integration test: JCP code drives Java's built-in HttpClient to call an
 * embedded Jetty server.
 *
 * <p>Exercises the full JCP↔Java type bridge with two third-party boundaries:
 * <ul>
 *   <li>Jetty 11 — embedded HTTP server registered in plain Java, not JCP (JCP isn't
 *       used to configure the server, only to make HTTP calls to it)</li>
 *   <li>java.net.http.HttpClient — registered in JCP; both static factory calls and
 *       instance method chains are exercised</li>
 * </ul>
 *
 * <p>The Jetty server is shared across all nested test classes and started once via
 * {@code @BeforeAll} on the outer class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpClientIntegrationTest {

    static final String RESPONSE_BODY = "Hello from JCP";
    static final String RESPONSE_JSON = "{\"status\":\"ok\",\"value\":42}";

    private static final AtomicInteger counter = new AtomicInteger();

    private Server server;
    private int port;

    private String genName(String base) {
        return base + "_" + counter.incrementAndGet();
    }

    @BeforeAll
    void startJetty() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0); // ephemeral port
        server.addConnector(connector);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                baseRequest.setHandled(true);
                if ("/json".equals(target)) {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(RESPONSE_JSON);
                } else {
                    response.setContentType("text/plain");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(RESPONSE_BODY);
                }
            }
        });
        server.start();
        port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    @AfterAll
    void stopJetty() throws Exception {
        server.stop();
    }

    // -------------------------------------------------------------------------
    // Eval mode
    // -------------------------------------------------------------------------

    @Nested
    class EvalMode {

        /**
         * Eval mode — GET plain-text endpoint.
         *
         * <pre>
         * client  = HttpClient.newHttpClient()
         * builder = HttpRequest.newBuilder()
         * builder = builder.uri(URI.create("http://localhost:{port}/"))
         * request = builder.build()
         * handler = HttpResponse.BodyHandlers.ofString()
         * resp    = client.send(request, handler)
         * result  = resp.body()           // → "Hello from JCP"
         * </pre>
         */
        @Test
        void getPlainText_evalMode_bodyMatchesResponse() throws Exception {
            RootEvalContext ctx = new RootEvalContext();
            // Register Builder before HttpClient: both have simple name "Builder"; whichever
            // registers first claims that key — we need HttpRequest.Builder.
            ClassConverter.registerClass(HttpRequest.Builder.class, ctx, "http");
            ClassConverter.registerClass(HttpClient.class, ctx, "http");
            ClassConverter.registerClass(HttpRequest.class, ctx, "http");
            ClassConverter.registerClass(HttpResponse.class, ctx, "http");
            ClassConverter.registerClass(HttpResponse.BodyHandlers.class, ctx, "http");
            ClassConverter.registerClass(URI.class, ctx, "http");

            String url = "http://localhost:" + port + "/";

            Block program = new BlockImpl();

            // client = HttpClient.newHttpClient()
            program.addStatement(new VariableDeclarationImpl("client", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpClient.newHttpClient"))));

            // builder = HttpRequest.newBuilder()
            program.addStatement(new VariableDeclarationImpl("builder", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpRequest.newBuilder"))));

            // uriObj = URI.create("http://localhost:{port}/")
            program.addStatement(new VariableDeclarationImpl("uriObj", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::URI.create"),
                            LiteralExpression.of(url))));

            // builder = builder.uri(uriObj)
            program.addStatement(new VariableDeclarationImpl("builder2", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::Builder.uri"),
                            new VariableExpression(Identifier.fromName("builder")),
                            new VariableExpression(Identifier.fromName("uriObj")))));

            // request = builder2.build()
            program.addStatement(new VariableDeclarationImpl("request", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::Builder.build"),
                            new VariableExpression(Identifier.fromName("builder2")))));

            // handler = HttpResponse.BodyHandlers.ofString()
            program.addStatement(new VariableDeclarationImpl("handler", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::BodyHandlers.ofString"))));

            // resp = client.send(request, handler)
            program.addStatement(new VariableDeclarationImpl("resp", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpClient.send"),
                            new VariableExpression(Identifier.fromName("client")),
                            new VariableExpression(Identifier.fromName("request")),
                            new VariableExpression(Identifier.fromName("handler")))));

            // result = resp.body()
            program.addStatement(new VariableDeclarationImpl("result", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpResponse.body"),
                            new VariableExpression(Identifier.fromName("resp")))));

            new EvalVisitor(ctx).visit(program);

            Object result = ctx.getVariable("result").get();
            assertEquals(RESPONSE_BODY, result, "Response body must match server output");
        }

        /**
         * Eval mode — GET JSON endpoint, assert statusCode and body.
         *
         * <pre>
         * client  = HttpClient.newHttpClient()
         * builder = HttpRequest.newBuilder()
         * builder = builder.uri(URI.create("http://localhost:{port}/json"))
         * request = builder.build()
         * handler = HttpResponse.BodyHandlers.ofString()
         * resp    = client.send(request, handler)
         * status  = resp.statusCode()    // → 200
         * result  = resp.body()          // → "{\"status\":\"ok\",\"value\":42}"
         * </pre>
         */
        @Test
        void getJson_evalMode_statusAndBodyMatch() throws Exception {
            RootEvalContext ctx = new RootEvalContext();
            ClassConverter.registerClass(HttpRequest.Builder.class, ctx, "http");
            ClassConverter.registerClass(HttpClient.class, ctx, "http");
            ClassConverter.registerClass(HttpRequest.class, ctx, "http");
            ClassConverter.registerClass(HttpResponse.class, ctx, "http");
            ClassConverter.registerClass(HttpResponse.BodyHandlers.class, ctx, "http");
            ClassConverter.registerClass(URI.class, ctx, "http");

            String url = "http://localhost:" + port + "/json";

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl("client", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpClient.newHttpClient"))));

            program.addStatement(new VariableDeclarationImpl("builder", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpRequest.newBuilder"))));

            program.addStatement(new VariableDeclarationImpl("uriObj", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::URI.create"),
                            LiteralExpression.of(url))));

            program.addStatement(new VariableDeclarationImpl("builder2", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::Builder.uri"),
                            new VariableExpression(Identifier.fromName("builder")),
                            new VariableExpression(Identifier.fromName("uriObj")))));

            program.addStatement(new VariableDeclarationImpl("request", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::Builder.build"),
                            new VariableExpression(Identifier.fromName("builder2")))));

            program.addStatement(new VariableDeclarationImpl("handler", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::BodyHandlers.ofString"))));

            program.addStatement(new VariableDeclarationImpl("resp", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpClient.send"),
                            new VariableExpression(Identifier.fromName("client")),
                            new VariableExpression(Identifier.fromName("request")),
                            new VariableExpression(Identifier.fromName("handler")))));

            program.addStatement(new VariableDeclarationImpl("status", SystemDataType.INT,
                    new FunctionCallExpression(Identifier.fromName("http::HttpResponse.statusCode"),
                            new VariableExpression(Identifier.fromName("resp")))));

            program.addStatement(new VariableDeclarationImpl("result", SystemDataType.ANY,
                    new FunctionCallExpression(Identifier.fromName("http::HttpResponse.body"),
                            new VariableExpression(Identifier.fromName("resp")))));

            new EvalVisitor(ctx).visit(program);

            assertEquals(200, ctx.getVariable("status").get(), "Status code must be 200");
            assertEquals(RESPONSE_JSON, ctx.getVariable("result").get(), "JSON body must match");
        }
    }

    // -------------------------------------------------------------------------
    // Compile mode
    // -------------------------------------------------------------------------

    @Nested
    class CompileMode {

        /**
         * Compile mode — GET plain-text endpoint via bytecode.
         *
         * <p>Registers HttpClient, HttpRequest.Builder, HttpRequest, HttpResponse,
         * HttpResponse.BodyHandlers, and URI into the compile context. Generates a
         * static {@code evaluate()} method that returns the response body as a String.
         *
         * <pre>
         * client  = HttpClient.newHttpClient()
         * builder = HttpRequest.newBuilder()
         * uriObj  = URI.create("http://localhost:{port}/")
         * builder = builder.uri(uriObj)
         * request = builder.build()
         * handler = BodyHandlers.ofString()
         * resp    = client.send(request, handler)
         * return  resp.body()
         * </pre>
         */
        @Test
        void getPlainText_compileMode_bodyMatchesResponse() throws Exception {
            String url = "http://localhost:" + port + "/";
            String className = genName("HttpGet");

            BytecodeGenerator gen = new BytecodeGenerator(className);
            // Register Builder before HttpClient: both share simple name "Builder".
            gen.registerExternalClass(HttpRequest.Builder.class);
            gen.registerExternalClass(HttpClient.class);
            gen.registerExternalClass(HttpRequest.class);
            gen.registerExternalClass(HttpResponse.class);
            gen.registerExternalClass(HttpResponse.BodyHandlers.class);
            gen.registerExternalClass(URI.class);

            Block program = new BlockImpl();

            // client = HttpClient.newHttpClient()
            program.addStatement(new VariableDeclarationImpl("client", SystemDataType.ANY,
                    new StaticMethodCallExpression("HttpClient", "newHttpClient")));

            // builder = HttpRequest.newBuilder()
            program.addStatement(new VariableDeclarationImpl("builder", SystemDataType.ANY,
                    new StaticMethodCallExpression("HttpRequest", "newBuilder")));

            // uriObj = URI.create(url)
            program.addStatement(new VariableDeclarationImpl("uriObj", SystemDataType.ANY,
                    new StaticMethodCallExpression("URI", "create",
                            LiteralExpression.of(url))));

            // builder2 = builder.uri(uriObj)
            program.addStatement(new VariableDeclarationImpl("builder2", SystemDataType.ANY,
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("builder")),
                            "uri",
                            new VariableExpression(Identifier.fromName("uriObj")))));

            // request = builder2.build()
            program.addStatement(new VariableDeclarationImpl("request", SystemDataType.ANY,
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("builder2")),
                            "build")));

            // handler = BodyHandlers.ofString()
            program.addStatement(new VariableDeclarationImpl("handler", SystemDataType.ANY,
                    new StaticMethodCallExpression("BodyHandlers", "ofString")));

            // resp = client.send(request, handler)
            program.addStatement(new VariableDeclarationImpl("resp", SystemDataType.ANY,
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("client")),
                            "send",
                            new VariableExpression(Identifier.fromName("request")),
                            new VariableExpression(Identifier.fromName("handler")))));

            // return resp.body()
            MethodCallExpression bodyExpr = new MethodCallExpression(
                    new VariableExpression(Identifier.fromName("resp")),
                    "body");

            byte[] bytecode = gen.compileWithReturn(program, bodyExpr, SystemDataType.ANY);

            MultiClassLoader loader = new MultiClassLoader();
            loader.defineClass(className, bytecode);
            Class<?> clazz = loader.loadClass(className);
            Object result = clazz.getMethod("evaluate").invoke(null);
            assertEquals(RESPONSE_BODY, result, "Compiled HTTP GET body must match server output");
        }

        /**
         * Compile mode — GET JSON endpoint, assert body contains expected JSON.
         */
        @Test
        void getJson_compileMode_bodyMatchesResponse() throws Exception {
            String url = "http://localhost:" + port + "/json";
            String className = genName("HttpGetJson");

            BytecodeGenerator gen = new BytecodeGenerator(className);
            gen.registerExternalClass(HttpRequest.Builder.class);
            gen.registerExternalClass(HttpClient.class);
            gen.registerExternalClass(HttpRequest.class);
            gen.registerExternalClass(HttpResponse.class);
            gen.registerExternalClass(HttpResponse.BodyHandlers.class);
            gen.registerExternalClass(URI.class);

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl("client", SystemDataType.ANY,
                    new StaticMethodCallExpression("HttpClient", "newHttpClient")));

            program.addStatement(new VariableDeclarationImpl("builder", SystemDataType.ANY,
                    new StaticMethodCallExpression("HttpRequest", "newBuilder")));

            program.addStatement(new VariableDeclarationImpl("uriObj", SystemDataType.ANY,
                    new StaticMethodCallExpression("URI", "create",
                            LiteralExpression.of(url))));

            program.addStatement(new VariableDeclarationImpl("builder2", SystemDataType.ANY,
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("builder")),
                            "uri",
                            new VariableExpression(Identifier.fromName("uriObj")))));

            program.addStatement(new VariableDeclarationImpl("request", SystemDataType.ANY,
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("builder2")),
                            "build")));

            program.addStatement(new VariableDeclarationImpl("handler", SystemDataType.ANY,
                    new StaticMethodCallExpression("BodyHandlers", "ofString")));

            program.addStatement(new VariableDeclarationImpl("resp", SystemDataType.ANY,
                    new MethodCallExpression(
                            new VariableExpression(Identifier.fromName("client")),
                            "send",
                            new VariableExpression(Identifier.fromName("request")),
                            new VariableExpression(Identifier.fromName("handler")))));

            MethodCallExpression bodyExpr = new MethodCallExpression(
                    new VariableExpression(Identifier.fromName("resp")),
                    "body");

            byte[] bytecode = gen.compileWithReturn(program, bodyExpr, SystemDataType.ANY);

            MultiClassLoader loader = new MultiClassLoader();
            loader.defineClass(className, bytecode);
            Class<?> clazz = loader.loadClass(className);
            Object result = clazz.getMethod("evaluate").invoke(null);
            assertEquals(RESPONSE_JSON, result, "Compiled HTTP GET JSON body must match");
        }
    }
}
