package com.example.customers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@SpringBootApplication
public class CustomersApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
	}

	private final String[] names = "Jean,Max,Maria,Dave,Jose,NiceWraith,Phil,Jinx,Puji,Washy,Ash".split(",");

	private final AtomicInteger counter = new AtomicInteger();

	private final Flux<Customer> customerFlux = Flux.fromStream(
			Stream.generate(() -> {
				var id = counter.incrementAndGet();
				return new Customer(id, names[id % names.length]);
			})).delayElements(Duration.ofSeconds(1));

	@Bean
	Flux<Customer> customers() {
		return this.customerFlux.publish().autoConnect();
	}
}

@Configuration
@RequiredArgsConstructor
class CustomerWebSocketConfiguration {

	private final ObjectMapper objectMapper;

	@SneakyThrows
	private String from(Customer customer) {
		return this.objectMapper.writeValueAsString(customer);
	}

	@Bean
	WebSocketHandler webSocketHandler(Flux<Customer> customerFlux) {
		return webSocketSession -> {
			Flux<WebSocketMessage> map = customerFlux.map(this::from)
					.map(webSocketSession::textMessage);
			return webSocketSession.send(map);
		};
	}

	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping (WebSocketHandler webSocketHandler) {
		return new SimpleUrlHandlerMapping(Map.of("/ws/customers", webSocketHandler), 10);
	}
}

@RestController
@RequiredArgsConstructor
class CustomerRestController {

	private final Flux<Customer> customerFlux;

	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/customers")
	Flux<Customer> get() {
		return customerFlux;
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
	private Integer id;
	private String name;
}

@RestController
class ReliabilityRestController {

	private final Map<String, AtomicInteger> countsOfErrors = new ConcurrentHashMap<>();
	private final Map<Long, AtomicInteger> countPerSecond = new ConcurrentHashMap<>();

	@GetMapping("/hello")
	String hello() {
		var now = System.currentTimeMillis();
		var second = (now/1000);
		var result = countPerSecond.compute(second, (aLong, atomicInteger) -> {
			if (atomicInteger == null) atomicInteger = new AtomicInteger(0);
			atomicInteger.incrementAndGet();
			return atomicInteger;
		});
		System.out.println("There have been "+ result.get() + " requests for the second " + second);
		return "hello()";
	}

	@GetMapping("/error/{id}")
	ResponseEntity<?> error(@PathVariable String id) {
		var result = this.countsOfErrors.compute(id, new BiFunction<>() {
			@Override
			public AtomicInteger apply(String s, AtomicInteger atomicInteger) {
				if (null == atomicInteger) atomicInteger = new AtomicInteger(0);
				atomicInteger.incrementAndGet();
				return atomicInteger;
			}
		});

		if( result.get() < 5) {
			System.out.println("error for ID '" + id + "' on count #" + result);
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		} else {
			return ResponseEntity.ok(Map.of("message", "good job, " + id +" you did in try nnumber " + result.get()));
		}
	}
}
