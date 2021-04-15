package org.acme;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;

@Path("")
public class RemoteResource {

    public static final int CALLS = 1000;
    public static final int CALL_DURATION_MS = 1000;
    
    private AtomicInteger counter = new AtomicInteger();
    
    @Path("fan-out")
    @GET
    public CompletionStage<String> fanOut(@Context UriInfo uriInfo) {
        URI uri = uriInfo.getBaseUriBuilder().path(RemoteResource.class, "remote").build();
        Client client = new ClientBuilderImpl().httpClientOptions(new HttpClientOptions().setMaxPoolSize(1000)).build();
        WebTarget target = client.target(uri);
        CompletionStageRxInvoker invocation = target.request().rx();
        StringBuilder sb = new StringBuilder();
        CompletableFuture<String>[] calls = new CompletableFuture[CALLS];
        for(int i=0;i<CALLS;i++) {
            calls[i] = invocation.get(String.class)
                    .whenComplete((val, t) -> sb.append(val).append(", "))
                    .toCompletableFuture();
        }
        return CompletableFuture.allOf(calls).thenApply(v -> sb.toString());
    }
    
    @Path("remote")
    @GET
    public Uni<Integer> remote() throws InterruptedException {
        return Uni.createFrom().item(() -> counter.getAndIncrement())
                .onItem().delayIt().by(Duration.ofMillis(CALL_DURATION_MS));
    }   
}