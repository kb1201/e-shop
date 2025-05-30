package hr.fer.dipl.client.recommendation;


import hr.fer.dipl.client.recommendation.model.RecommendationRequest;
import hr.fer.dipl.client.recommendation.model.RecommendationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "recommendation-service", url = "${recommendation-service.url:http://localhost:8085}")
public interface RecommendationSystemClient {
    @PostMapping("/api/v1/recommendations")
    RecommendationResponse getRecommendations(
            @RequestBody RecommendationRequest request
    );
}