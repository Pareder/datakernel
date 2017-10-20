/*
 * Copyright (C) 2015 SoftIndex LLC.
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

package io.datakernel.http;

import io.datakernel.async.Stages;
import io.datakernel.bytebuf.ByteBufStrings;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.loader.FileNamesLoadingService;
import io.datakernel.loader.ResourcesNameLoadingService;
import io.datakernel.loader.StaticLoaders;
import io.datakernel.loader.StaticLoader;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.*;

import static io.datakernel.bytebuf.ByteBufPool.*;
import static io.datakernel.bytebuf.ByteBufStrings.decodeAscii;
import static io.datakernel.bytebuf.ByteBufStrings.encodeAscii;
import static io.datakernel.eventloop.FatalErrorHandlers.rethrowOnAnyError;
import static io.datakernel.http.HttpRequest.get;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class StaticServletsTest {
    public static final String EXPECTED_CONTENT = "This is a test string";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @ClassRule
    public static final TemporaryFolder tmpFolder = new TemporaryFolder();

    private static Path resourcesPath;
    private static File resourcesFile;


    @BeforeClass
    public static void setup() throws IOException {
        resourcesPath = tmpFolder.newFolder("static").toPath();
        resourcesFile = resourcesPath.toFile();

        Files.write(resourcesPath.resolve("index.html"), encodeAscii(EXPECTED_CONTENT));
    }

    @Test
    public void testPathLoader() throws ExecutionException, InterruptedException {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofPath(eventloop, executor, resourcesPath);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/index.html");
        CompletableFuture<String> future = servlet.serve(request)
                .thenApply(httpResponse -> decodeAscii(httpResponse.getBody()))
                .toCompletableFuture();

        eventloop.run();

        assertEquals(EXPECTED_CONTENT, future.get());
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());
    }

    @Test
    public void testFileNotFoundPathLoader() throws Throwable {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofPath(eventloop, executor, resourcesPath);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/anknownFile.txt");
        CompletableFuture<HttpResponse> future = servlet.serve(request).toCompletableFuture();

        eventloop.run();

        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());

        exception.expectCause(instanceOf(HttpException.class));
        exception.expectCause(hasProperty("code", is(404)));

        future.get();
    }

    @Test
    public void testFileLoader() throws ExecutionException, InterruptedException {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofFile(eventloop, executor, resourcesFile);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/index.html");
        CompletableFuture<String> future = servlet.serve(request)
                .thenApply(httpResponse -> decodeAscii(httpResponse.getBody()))
                .toCompletableFuture();

        eventloop.run();

        assertEquals(EXPECTED_CONTENT, future.get());
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());
    }

    @Test
    public void testFileNotFoundCachedFileLoader() throws Throwable {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofFile(eventloop, executor, resourcesFile);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/testFile.txt");
        CompletableFuture<HttpResponse> future = servlet.serve(request).toCompletableFuture();

        eventloop.run();

        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());

        exception.expectCause(instanceOf(HttpException.class));
        exception.expectCause(hasProperty("code", is(404)));

        future.get();
    }

    @Test
    public void testClassPath() throws ExecutionException, InterruptedException {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofClassPath(eventloop, executor);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/testFile.txt");
        CompletableFuture<String> future = servlet.serve(request)
                .thenApply(httpResponse -> decodeAscii(httpResponse.getBody()))
                .toCompletableFuture();

        eventloop.run();

        assertEquals(EXPECTED_CONTENT, future.get());
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());
    }

    @Test
    public void testFileNotFoundClassPath() throws Throwable {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofClassPath(eventloop, executor);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/index.html");
        CompletableFuture<HttpResponse> future = servlet.serve(request).toCompletableFuture();

        eventloop.run();
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());

        exception.expectCause(instanceOf(HttpException.class));
        exception.expectCause(hasProperty("code", is(404)));

        future.get();
    }

    @Test
    public void testRelativeClassPath() throws ExecutionException, InterruptedException {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofClassPath(eventloop, executor, this.getClass());
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/testFile.txt");
        CompletableFuture<String> future = servlet.serve(request)
                .thenApply(httpResponse -> decodeAscii(httpResponse.getBody()))
                .toCompletableFuture();

        eventloop.run();

        assertEquals(EXPECTED_CONTENT, future.get());
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());
    }

    @Test
    public void testFileNotFoundRelativeClassPath() throws Throwable {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        StaticLoader resourceLoader = StaticLoaders.ofClassPath(eventloop, executor, StaticServlet.class);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/unknownFile.txt");
        CompletableFuture<HttpResponse> future = servlet.serve(request).toCompletableFuture();

        eventloop.run();
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());

        exception.expectCause(instanceOf(HttpException.class));
        exception.expectCause(hasProperty("code", is(404)));

        future.get();
    }

    @Test
    public void testResourcesNameLoadingService() throws InterruptedException, ExecutionException {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ResourcesNameLoadingService preDownloadResources = ResourcesNameLoadingService.create(eventloop,
                executor, classLoader, "dir2");
        preDownloadResources.start();

        eventloop.run();

        StaticLoader testLoader = name -> name.equals("dir2/testFile.txt")
                ? Stages.of(ByteBufStrings.wrapAscii(EXPECTED_CONTENT))
                : Stages.ofException(new NoSuchFileException(name));

        StaticLoader resourceLoader = StaticLoaders.ofPredicate(testLoader, preDownloadResources::contains);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        HttpRequest request = get("http://test.com:8080/dir2/testFile.txt");
        CompletableFuture<String> future = servlet.serve(request)
                .thenApply(httpResponse -> decodeAscii(httpResponse.getBody()))
                .toCompletableFuture();

        eventloop.run();

        assertEquals(EXPECTED_CONTENT, future.get());
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());
    }

    @Test
    public void testFileNamesLoadingService() throws InterruptedException, ExecutionException {
        Eventloop eventloop = Eventloop.create().withFatalErrorHandler(rethrowOnAnyError());
        ExecutorService executor = Executors.newCachedThreadPool();

        FileNamesLoadingService fileService = FileNamesLoadingService.create(eventloop, executor, resourcesPath);
        fileService.start();
        eventloop.run();

        StaticLoader testLoader = name -> name.equals("index.html")
                ? Stages.of(ByteBufStrings.wrapAscii(EXPECTED_CONTENT))
                : Stages.ofException(new NoSuchFileException(name));

        StaticLoader resourceLoader = StaticLoaders.ofPredicate(testLoader, fileService::contains);
        StaticServlet servlet = StaticServlet.create(eventloop, resourceLoader);

        CompletableFuture<String> future = servlet.serve(get("http://test.com:8080/index.html"))
                .thenApply(httpResponse -> decodeAscii(httpResponse.getBody()))
                .toCompletableFuture();

        eventloop.run();
        String content = future.get();
        assertEquals(EXPECTED_CONTENT, content);
        assertEquals(getPoolItemsString(), getCreatedItems(), getPoolItems());
    }
}