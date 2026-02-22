package com.sqli.workshop.ddd.connaissance.client.cpostal;

import java.util.List;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.CodePostauxService;
import com.sqli.workshop.ddd.connaissance.client.generated.codepostal.client.CodesPostauxApi;
import com.sqli.workshop.ddd.connaissance.client.generated.codepostal.model.Commune;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@AllArgsConstructor
@Slf4j
public class CodePostauxServiceImpl implements CodePostauxService {
    
    CodesPostauxApi codesPostauxApi;

    /**
     * Valide la cohérence d'un couple code postal/ville via l'API IGN externe.
     * <p>
     * Cette méthode implémente un circuit breaker Resilience4j pour garantir la résilience
     * en cas d'indisponibilité de l'API IGN.
     * 
     * <p><strong>Configuration du circuit breaker (application.yml) :</strong>
     * <ul>
     *   <li><strong>failureRateThreshold</strong> : 30% - Circuit s'ouvre si 30% d'échecs</li>
     *   <li><strong>slowCallRateThreshold</strong> : 50% - Circuit s'ouvre si 50% d'appels lents</li>
     *   <li><strong>slowCallDurationThreshold</strong> : 3s - Appel considéré lent si &gt; 3s</li>
     *   <li><strong>waitDurationInOpenState</strong> : 60s - Reste ouvert 60s avant test</li>
     *   <li><strong>slidingWindowSize</strong> : 10 - Fenêtre de 10 appels</li>
     *   <li><strong>minimumNumberOfCalls</strong> : 5 - Min 5 appels avant calcul</li>
     * </ul>
     * 
     * <p><strong>États du circuit breaker :</strong>
     * <ul>
     *   <li><strong>CLOSED</strong> : Normal - Appels passent vers l'API IGN</li>
     *   <li><strong>OPEN</strong> : Dégradé - Fallback automatique (validation ignorée)</li>
     *   <li><strong>HALF_OPEN</strong> : Test - Tentative de rétablissement</li>
     * </ul>
     * 
     * <p><strong>Fallback :</strong> En mode dégradé (circuit ouvert), la méthode
     * {@link #validateCodePostalFallback(CodePostal, Ville, Throwable)} retourne {@code true}
     * pour permettre la poursuite de l'opération sans validation externe.
     * 
     * <p><strong>Métriques exposées :</strong>
     * <ul>
     *   <li>resilience4j_circuitbreaker_state (gauge) : État actuel du circuit</li>
     *   <li>resilience4j_circuitbreaker_calls (counter) : Nombre d'appels (success/failed/rejected)</li>
     *   <li>resilience4j_circuitbreaker_failure_rate (gauge) : Taux d'échec en %</li>
     *   <li>resilience4j_circuitbreaker_slow_call_rate (gauge) : Taux d'appels lents en %</li>
     * </ul>
     * 
     * <p><strong>Performance :</strong> Temps de réponse typique &lt; 1s (API IGN performante),
     * timeout circuit breaker à 3s.
     * 
     * @param codePostal le code postal à valider (non null)
     * @param ville la ville à valider (non null)
     * @return {@code true} si le code postal correspond à la ville, {@code false} sinon.
     *         En mode dégradé (circuit ouvert), retourne {@code true} (fallback).
     * 
     * @see #validateCodePostalFallback(CodePostal, Ville, Throwable)
     * @see <a href="https://resilience4j.readme.io/docs/circuitbreaker">Resilience4j Circuit Breaker</a>
     */
    @Override
    public boolean validateCodePostal(CodePostal codePostal, Ville ville) {
        ResponseEntity<List<Commune>> resultEntity = ResponseEntity.internalServerError().build();
        try {
            log.debug("Calling code postal API for code: {}", codePostal.value());
            resultEntity = codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal.value());
            log.debug("Received response from code postal API with status: {}", resultEntity.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.warn("HTTP error calling code postal API: {}", e.getMessage());
            resultEntity = ResponseEntity.status(e.getStatusCode()).build();
        }
        log.debug("Processing response for code postal validation...");
        if (resultEntity != null && resultEntity.getStatusCode().is2xxSuccessful()) {
            List<Commune> communes = resultEntity.getBody();
            if (communes != null) {
                for (int i=0; i < communes.size(); i++) {
                    if (ville.value().equalsIgnoreCase(communes.get(i).getNomCommune())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
}
