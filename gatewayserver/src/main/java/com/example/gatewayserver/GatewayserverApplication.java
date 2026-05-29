package com.example.gatewayserver;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayserverApplication {

    public static void main(String[] args) {

        SpringApplication.run(GatewayserverApplication.class, args);

    }


    //Implementing Custom Routing in Spring Cloud gateway Server
    @Bean
    public RouteLocator BanklyCoreRouteConfig(RouteLocatorBuilder routeLocatorBuilder) {

        return routeLocatorBuilder.routes()
                .route(p -> p.path("/banklycore/accounts/**")
                        .filters(f -> f.rewritePath("/banklycore/accounts/(?<segment>.*)", "/${segment}")
                                .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                                .circuitBreaker(config -> config.setName("accountsCircuitBreaker").setFallbackUri("forward:/contact-support")))
                        .uri("lb://ACCOUNTS"))

                .route(p -> p.path("/banklycore/loans/**")
                        .filters(f -> f.rewritePath("/banklycore/loans/(?<segment>.*)", "/${segment}")
                                .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                                .retry(retryConfig -> retryConfig.setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true))
                        )
                        .uri("lb://LOANS"))

                .route(p -> p.path("/banklycore/cards/**")
                        .filters(f -> f.rewritePath("/banklycore/cards/(?<segment>.*)", "/${segment}")
                                        .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                                        .requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter())
                                                .setKeyResolver(userkeyResolver()))
                                //   .circuitBreaker(config -> config.setName("cardsCircuitBreaker"))
                        )
                        .uri("lb://CARDS")).build();
    }


    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(4))
                                .build()
                )
                .build());
    }



    //http://localhost:8072/actuator/circuitbreakerevents?name=accountsCircuitBreaker ----> This to know about all the circuit Breaker Events

    // http://localhost:8072/actuator/circuitbreakers ---> this to get to know what state circuit Breaker State is in(Open, Close, Half Open )


    // ----------------- FOR PER-TIMEOUT configurations using Java DSL---------------------
    //
    //      @Bean
    //      public RouteLocator customRouteLocator(RouteLocatorBuilder routeBuilder){
    //         return routeBuilder.routes()
    //               .route("test1", r -> {
    //                  return r.host("*.somehost.org").and().path("/somepath")
    //                        .filters(f -> f.addRequestHeader("header1", "header-value-1"))
    //                        .uri("http://someuri")
    //                        .metadata(RESPONSE_TIMEOUT_ATTR, 200)
    //                        .metadata(CONNECT_TIMEOUT_ATTR, 200);
    //               })
    //               .build();
    //      }
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 1, 1);
    }

    @Bean
    KeyResolver userkeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("user"))
                .defaultIfEmpty("anonymous");
    }

    //ab -n 10 -c 2 -v 3 http://localhost:8072/banklycore/cards/api/contact-info
    //Testing Rate limiting using Apache benchmark
    // n : no of requests
    // c: no of Concurrent Request i.e. how many requests at one time
    // v: stands for verbosity level. It controls how much debugging and troubleshooting information
    //                                the tool prints to your console while the test is running.


}