import io.datakernel.async.Promise;
import io.datakernel.async.Promises;
import io.datakernel.csp.ChannelConsumer;
import io.datakernel.csp.ChannelSupplier;
import io.datakernel.csp.queue.ChannelBuffer;
import io.datakernel.csp.queue.ChannelQueue;
import io.datakernel.csp.queue.ChannelZeroBuffer;
import io.datakernel.eventloop.Eventloop;

/**
 * @author Alex Syrotenko (@pantokrator)
 * Created on 18.07.19.
 */
public final class ChannelBufferExample {
	//[START REGION_1]
	static final class ChannelBufferStream {
		public static void main(String[] args) {
			Eventloop eventloop = Eventloop.create().withCurrentThread();

			ChannelBuffer<Integer> plate = new ChannelBuffer<>(5, 10);
			ChannelSupplier<Integer> granny = plate.getSupplier();
			Promises.loop(0, apple -> Promise.of(apple < 25), apple ->
					plate.put(apple).map($ -> {
						System.out.println("Granny gives apple   #" + apple);
						return apple + 1;
					}));
			granny.streamTo(ChannelConsumer.ofConsumer(apple -> System.out.println("Grandson takes apple #" + apple)));
			eventloop.run();
		}
	}
	//[END REGION_1]

	//[START REGION_2]
	static final class ChannelBufferZeroExample {
		public static void main(String[] args) {
			Eventloop eventloop = Eventloop.create().withCurrentThread();

			ChannelQueue<Integer> buffer = new ChannelZeroBuffer<>();
			ChannelSupplier<Integer> granny = buffer.getSupplier();

			Promises.loop(0, apple -> Promise.of(apple < 10), apple ->
					buffer.put(apple).map($ -> {
						System.out.println("Granny gives apple   #" + apple);
						return apple + 1;
					}));

			granny.streamTo(ChannelConsumer.<Integer>ofConsumer((apple) ->
					System.out.println("Grandson takes apple #" + apple)).async());

			eventloop.run();
		}
	}
	//[END REGION_2]
}
