package com.example.basics;

import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.SetPathGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * http://localhost:9999/actuator/gateway/routes
 * http://localhost:9999/actuator/
 * http://localhost:9999/actuator/metrics
 * http://localhost:9999/actuator/metrics/spring.cloud.gateway.requests
 */
@SpringBootApplication
public class DiscoveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryApplication.class, args);
	}

	//applicable to all routes
	/*@Bean
	GlobalFilter*/

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb
				.routes()
				.route(routeSpec -> routeSpec
						.path("/error/**")
						.filters(fs -> fs.retry(5))
						.uri("lb://customers"))
				.build();
	}

	/*@Bean
	ApplicationListener<RefreshRoutesResultEvent> refreshRoutesResultEventApplicationListener() {
		return refreshRoutesResultEvent -> {
			System.out.println("Routes updated");
			var crl = (CachingRouteLocator) refreshRoutesResultEvent.getSource();
			Flux<Route> routes = crl.getRoutes();
			routes.subscribe(System.out::println);
		};
	}*/

	//configure RouteLocatorBuilder yourself
	/*@Bean
	RouteLocator gateway(SetPathGatewayFilterFactory factory) {
		var singleRoute = Route.async()
				.id("test-route")
				.filters(new OrderedGatewayFilter(factory.apply(config -> config.setTemplate("/customer")), 1))
				.uri("lb://customers")
				.asyncPredicate(serverWebExchange -> {
					var uri = serverWebExchange.getRequest().getURI();
					var path = uri.getPath();
					var match = path.contains("/customer");
					return Mono.just(match);
				}).build();

		return () -> Flux.just(singleRoute);
	}*/

	/*@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb
				.routes()
				.route(routeSpec -> routeSpec
						.path("/customer")
						.uri("lb://customers"))
				.build();
	}*/

	/*@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb
				.routes()
				.route(routeSpec -> routeSpec.path("/hello")
						.and().host("*.spring.io") // curl http://localhost:9999/hello -H"Host: test.spring.io" -v
						.filters(gatewayFilterSpec -> gatewayFilterSpec.setPath("/guides")).uri("http://spring.io"))
				.route("twitter", routeSpec -> routeSpec.path("/twitter/**")
						.filters(fs -> fs.rewritePath("twitter/(?<handle>.*)", "/${handle}"))
				.uri("http://twitter.com/@"))
				.build();
	}*/

}
