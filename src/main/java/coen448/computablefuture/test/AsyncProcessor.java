package coen448.computablefuture.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AsyncProcessor {
	
    public CompletableFuture<String> processAsync(List<Microservice> microservices, String message) {
    	
        List<CompletableFuture<String>> futures = microservices.stream()
            .map(client -> client.retrieveAsync(message))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining(" ")));
        
    }
    
    public CompletableFuture<List<String>> processAsyncCompletionOrder(
            List<Microservice> microservices, String message) {

        List<String> completionOrder =
            Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = microservices.stream()
            .map(ms -> ms.retrieveAsync(message)
                .thenAccept(completionOrder::add))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> completionOrder);
        
    }

    public CompletableFuture<String> processAsyncFailFast(
        List<Microservice> services,
        List<String> messages) {

            List<CompletableFuture<String>> future_services = new ArrayList<>();

            for (int i = 0; i < services.size(); i++) {
                
                future_services.add(services.get(i).retrieveAsync(messages.get(i)));

            }

            CompletableFuture<Void> all_failures = CompletableFuture.allOf(
                future_services.toArray(new CompletableFuture[0])
            );

            return all_failures.thenApply(v -> future_services.stream()
                                                .map(CompletableFuture::join)
                                                .collect(Collectors.joining(" "))
                                    );                
                                                 
    }

    public CompletableFuture<List<String>> processAsyncFailPartial(
        List<Microservice> services,
        List<String> messages
    ){
        List<CompletableFuture<String>> future_services =new ArrayList<>();

        for(int i=0; i < services.size(); i++){

            Microservice micro_service = services.get(i);
            String message = messages.get(i);

            //Failures are handled for each service
            CompletableFuture<String> future_failure = micro_service.retrieveAsync(message).handle((result,exception) -> {
                if(exception != null) {
                    return null;
                }
                return result;
            });
            future_services.add(future_failure);
        }

        return CompletableFuture.allOf(future_services.toArray(new CompletableFuture[0]))
                                    .thenApply(v -> future_services.stream()
                                                    .map(CompletableFuture::join)
                                                    .filter(result ->result!=null) //Only keep successful result
                                                    .collect(Collectors.toList())
                                    ); 
    }

    public CompletableFuture<String> processAsyncFailSoft(

        List<Microservice> services,
        List<String> messages,
        String fallbackValue){

            List<CompletableFuture<String>> future_services =new ArrayList<>();

            for(int i=0; i < services.size(); i++){

                Microservice micro_service = services.get(i);
                String message = messages.get(i);

                CompletableFuture<String> future_failure = micro_service.retrieveAsync(message).handle((result,exception) -> {
                    if(exception != null) {
                        return fallbackValue; //return fallback value instead of failure
                    }
                    return result;
                });
                future_services.add(future_failure);

            }

            return CompletableFuture.allOf(future_services.toArray(new CompletableFuture[0]))
                                    .thenApply(v -> future_services.stream()
                                                    .map(CompletableFuture::join)
                                                    .collect(Collectors.joining(" "))
                                    );

        }

    
    

    
}