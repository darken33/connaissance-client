package com.sqli.workshop.ddd.connaissance.client.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Health indicator pour le circuit breaker API IGN.
 * 
 * <p>Expose l'état du circuit breaker dans le endpoint /actuator/health :
 * <ul>
 *   <li>UP : Circuit fermé, l'API IGN est disponible</li>
 *   <li>DOWN : Circuit ouvert, l'API IGN est indisponible (fallback actif)</li>
 *   <li>UNKNOWN : Circuit half-open, en phase de test</li>
 * </ul>
 * 
 * <p>Métriques exposées :
 * <ul>
 *   <li>state : État actuel du circuit (CLOSED, OPEN, HALF_OPEN)</li>
 *   <li>failureRate : Taux d'échec en pourcentage</li>
 *   <li>slowCallRate : Taux d'appels lents en pourcentage</li>
 *   <li>bufferedCalls : Nombre d'appels dans la fenêtre glissante</li>
 *   <li>failedCalls : Nombre d'appels échoués</li>
 *   <li>slowCalls : Nombre d'appels lents</li>
 *   <li>notPermittedCalls : Nombre d'appels rejetés (circuit ouvert)</li>
 * </ul>
 * 
 * @see io.github.resilience4j.circuitbreaker.CircuitBreaker
 */
@Component
public class ApiIgnHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ApiIgnHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
/**
 * health() - TODO: description
 *
 * @todo Add detailed Javadoc
 */
    public Health health() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("apiIgn");
        
        CircuitBreaker.State state = circuitBreaker.getState();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        
        Health.Builder healthBuilder = switch (state) {
            case CLOSED -> Health.up()
                .withDetail("message", "API IGN est disponible");
            case OPEN -> Health.down()
                .withDetail("message", "API IGN est indisponible - Fallback actif");
            case HALF_OPEN -> Health.unknown()
                .withDetail("message", "API IGN en cours de test");
            default -> Health.unknown()
                .withDetail("message", "État du circuit breaker inconnu");
        };
        
        // Add circuit breaker metrics
        healthBuilder
            .withDetail("state", state.name())
            .withDetail("failureRate", "%.2f%%".formatted(metrics.getFailureRate()))
            .withDetail("slowCallRate", "%.2f%%".formatted(metrics.getSlowCallRate()))
            .withDetail("bufferedCalls", metrics.getNumberOfBufferedCalls())
            .withDetail("failedCalls", metrics.getNumberOfFailedCalls())
            .withDetail("slowCalls", metrics.getNumberOfSlowCalls())
            .withDetail("successfulCalls", metrics.getNumberOfSuccessfulCalls())
            .withDetail("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
        
        return healthBuilder.build();
    }
}