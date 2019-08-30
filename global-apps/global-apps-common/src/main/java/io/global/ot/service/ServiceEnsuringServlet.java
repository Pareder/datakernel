package io.global.ot.service;

import io.datakernel.async.Promise;
import io.datakernel.exception.ParseException;
import io.datakernel.exception.UncheckedException;
import io.datakernel.http.AsyncServlet;
import io.datakernel.http.HttpRequest;
import io.datakernel.http.HttpResponse;
import io.global.common.PrivKey;
import org.jetbrains.annotations.NotNull;

public final class ServiceEnsuringServlet implements AsyncServlet {
	private final ContainerManager<?> serviceHolder;
	private final AsyncServlet next;

	private ServiceEnsuringServlet(ContainerManager serviceHolder, AsyncServlet next) {
		this.serviceHolder = serviceHolder;
		this.next = next;
	}

	public static ServiceEnsuringServlet create(ContainerManager serviceHolder, AsyncServlet next) {
		return new ServiceEnsuringServlet(serviceHolder, next);
	}

	@NotNull
	@Override
	public Promise<HttpResponse> serve(@NotNull HttpRequest request) throws UncheckedException {
		return Promise.complete()
				.then($ -> {
					try {
						String key = request.getCookie("Key");
						if (key != null) {
							PrivKey privKey = PrivKey.fromString(key);
							// return serviceHolder.ensureUserContainer(privKey);
							return Promise.complete();
						} else {
							return Promise.complete();
						}
					} catch (ParseException e) {
						return Promise.ofException(e);
					}
				})
				.then($ -> next.serve(request));
	}
}
