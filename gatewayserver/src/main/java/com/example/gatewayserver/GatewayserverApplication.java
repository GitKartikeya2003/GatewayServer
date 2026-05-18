package com.example.gatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

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
                                //  .circuitBreaker(config -> config.setName("loansCircuitBreaker"))
                        )
                        .uri("lb://LOANS"))

                .route(p -> p.path("/banklycore/cards/**")
                        .filters(f -> f.rewritePath("/banklycore/cards/(?<segment>.*)", "/${segment}")
                                        .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                                //   .circuitBreaker(config -> config.setName("cardsCircuitBreaker"))
                        )
                        .uri("lb://CARDS")).build();
    }

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