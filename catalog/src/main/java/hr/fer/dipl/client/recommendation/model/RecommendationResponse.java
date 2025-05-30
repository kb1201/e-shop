package hr.fer.dipl.client.recommendation.model;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecommendationResponse {
    private List<Long> recommendations;
    private Pagination pagination;
    private Metadata metadata;
}