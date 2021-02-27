package com.example.basics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * http://localhost:9999/actuator/gateway/routes
 * http://localhost:9999/actuator/
 * http://localhost:9999/actuator/metrics
 * http://localhost:9999/actuator/metrics/spring.cloud.gateway.requests
 *
 * curl -u bob:bob http://localhost:9999/hello
 * while true; do curl -u bob:bob http://localhost:9999/hello; done
 */
@SpringBootApplication
public class DiscoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryApplication.class, args);
    }

    @Bean
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(5, 10);
    }

    @Bean
    SecurityWebFilterChain authorization(ServerHttpSecurity http) {
        return http
                .httpBasic(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ae -> ae.pathMatchers("/hello").authenticated().anyExchange().permitAll())
                .build();
    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        return new MapReactiveUserDetailsService(User.withDefaultPasswordEncoder()
                .username("bob")
                .password("bob")
                .roles("USER")
                .build()
        );
    }

    //applicable to all routes
	/*@Bean
	GlobalFilter*/

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(rs -> rs
                        .path("/hello")
                        .filters(fs -> fs.requestRateLimiter(rlc -> rlc
                                .setRateLimiter(redisRateLimiter())
                                .setKeyResolver(/*exchange -> Mono.just("mycount")*/
                                        /*exchange -> exchange.getPrincipal().map(Principal::getName).switchIfEmpty(Mono.empty())*/new PrincipalNameKeyResolver())))
                        .uri("lb://customers"))
                .route(rs -> rs.path("/default")
                        .filters(fs -> fs.filter((exchange, chain) -> {
                            System.out.println("This is your second chance");
                            return chain.filter(exchange);
                        }))
                        .uri("https://spring.io/guids"))
                .route(rs -> rs.path("/customers")
                        .filters(fs -> fs.circuitBreaker(cbc -> cbc.setFallbackUri("forward:/default")))
                        .uri("lb://customers"))
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
