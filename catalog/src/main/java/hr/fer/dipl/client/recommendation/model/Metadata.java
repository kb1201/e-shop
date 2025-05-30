package hr.fer.dipl.client.recommendation.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Metadata {
    private Long userId;
    private String algorithm;
    private String generatedAt;
    private int cacheSizeUsed;
    private int actualRecommendations;
    private int pageItems;

}