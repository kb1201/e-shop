package hr.fer.dipl.client.recommendation.model;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecommendationRequest {
    private Long userId;
    private int page;
    private int pageSize;
}
