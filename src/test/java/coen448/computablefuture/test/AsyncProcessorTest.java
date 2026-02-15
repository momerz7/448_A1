package coen448.computablefuture.test;


import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AsyncProcessorTest {

    //helper class needed to handle forced failures, no Mockito allowed
    static class FailureHelper extends Microservice{

        public FailureHelper(String id){super(id);}

        @Override
        public CompletableFuture<String> retrieveAsync(String in){
            return CompletableFuture.failedFuture(new RuntimeException("boom"));
        }

    }

	@RepeatedTest(5)
    public void testProcessAsyncSuccess() throws Exception {
        
		Microservice service1 = new Microservice("Hello");
        Microservice service2 = new Microservice("World");
        

        AsyncProcessor processor = new AsyncProcessor();
        
        
        String result = processor.processAsync(List.of(service1,service2), "msg")
                                 .get(1,TimeUnit.SECONDS);

        assertEquals("Hello:MSG World:MSG", result);

        
    }
	
	
	@ParameterizedTest
    @CsvSource({
        "hi, Hello:HI World:HI",
        "cloud, Hello:CLOUD World:CLOUD",
        "async, Hello:ASYNC World:ASYNC"
    })
    public void testProcessAsync_withDifferentMessages(
            String message,
            String expectedResult)
            throws Exception {

        Microservice service1 = new Microservice("Hello");
        Microservice service2 = new Microservice("World");

        AsyncProcessor processor = new AsyncProcessor();

        String result = processor.processAsync(List.of(service1,service2), message)
                                 .get(1,TimeUnit.SECONDS);


        assertEquals(expectedResult, result);
        
    }
	
	
	@RepeatedTest(20)
    void showNondeterminism_completionOrderVaries() throws Exception {

        Microservice s1 = new Microservice("A");
        Microservice s2 = new Microservice("B");
        Microservice s3 = new Microservice("C");

        AsyncProcessor processor = new AsyncProcessor();

        List<String> order = processor
            .processAsyncCompletionOrder(List.of(s1, s2, s3), "msg")
            .get(1, TimeUnit.SECONDS);

        // Not asserting a fixed order (because it is intentionally nondeterministic)
        System.out.println(order);

        // A minimal sanity check: all three must be present
        assertEquals(3, order.size());

        assertTrue(order.stream().anyMatch(x -> x.startsWith("A:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("B:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("C:")));
    }

    //FAIL-FAST POLICY UNIT TESTING
    @Test
    void testFailurePropagates(){

        Microservice success = new Microservice("SUCCESS");
        Microservice fail = new FailureHelper("FAILURE");

        AsyncProcessor processor = new AsyncProcessor();

        assertThrows(
            ExecutionException.class, ()->processor.processAsyncFailFast(List.of(success,fail), List.of("x","y"))
                                                    .get(1, TimeUnit.SECONDS)
        );
    }

    @Test
    void testSucessFailFast() throws Exception{

        Microservice s1 = new Microservice("A");
        Microservice s2 = new Microservice("B");

        AsyncProcessor processor = new AsyncProcessor();

        String result = processor.processAsyncFailFast(List.of(s1,s2), List.of("x","y"))
                                 .get(1,TimeUnit.SECONDS);

        assertEquals("A:X B:Y", result);

    }


    //FAIL-PARTIAL POLICY UNIT TESTING
    @Test
    void testOnlySuccess_failPartial() throws Exception{

        Microservice s1 = new Microservice("A");
        Microservice s2 = new FailureHelper("BAD");
        Microservice s3 = new Microservice("C");

        AsyncProcessor processor = new AsyncProcessor();

        List<String> result = processor.processAsyncFailPartial(List.of(s1,s2,s3), List.of("x","y","z"))
                                        .get(1, TimeUnit.SECONDS);

        assertEquals(List.of("A:X","C:Z"),result);                                

    }

    @Test
    void testEmptyList_failPartial() throws Exception{

        Microservice f1 = new FailureHelper("X");
        Microservice f2 = new FailureHelper("Y");

        AsyncProcessor processor = new AsyncProcessor();

        List<String> result = processor.processAsyncFailPartial(List.of(f1,f2), List.of("a","b"))
                                        .get(1, TimeUnit.SECONDS);

        assertTrue(result.isEmpty());

    }


    //FAIL-SOFT POLICY UNIT TESTING
    @Test
    void testFallbackFailure_failsoft() throws Exception{

        Microservice s1 = new Microservice("A");
        Microservice s2 = new FailureHelper("BAD");

        AsyncProcessor processor = new AsyncProcessor();

        String result = processor.processAsyncFailSoft(List.of(s1,s2), List.of("x","y"), "FallBack")
                                 .get(1,TimeUnit.SECONDS);

        assertEquals("A:X FallBack",result);                         

    }

    @Test
    void testAllFail_failsoft() throws Exception{

        Microservice f1 = new FailureHelper("X");
        Microservice f2 = new FailureHelper("Y");

        AsyncProcessor processor = new AsyncProcessor();

        String result = processor.processAsyncFailSoft(List.of(f1,f2), List.of("a","b"), "Fallback")
                                        .get(1, TimeUnit.SECONDS);

        assertEquals("Fallback Fallback",result);

    }
}
	