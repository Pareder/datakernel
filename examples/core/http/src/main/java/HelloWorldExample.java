import io.datakernel.eventloop.Eventloop;
import io.datakernel.http.AsyncHttpServer;
import io.datakernel.http.HttpResponse;
import io.datakernel.promise.Promise;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class HelloWorldExample {
	private static final byte[] HELLO_WORLD = "Hello world!".getBytes(UTF_8);

	//[START REGION_1]
	public static void main(String[] args) throws IOException {
		Eventloop eventloop = Eventloop.create();
		AsyncHttpServer server = AsyncHttpServer.create(eventloop,
				request -> Promise.of(
						HttpResponse.ok200()
								.withBody(HELLO_WORLD)))
				.withListenPort(8080);

		server.listen();

		System.out.println("Server is running");
		System.out.println("You can connect from browser by visiting 'http://localhost:8080/'");

		eventloop.run();
	}
	//[END REGION_1]
}
