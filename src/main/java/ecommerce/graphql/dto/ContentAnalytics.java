package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ContentAnalytics {
    private long totalPageViews;
    private long totalClicks;
    private String avgTimeOnPage;
    private long uniqueVisitors;
    private String bounceRate;
    private int articlesPublished;
    private String pageViewsChange;
    private String clicksChange;
    private String timeOnPageChange;
    private String visitorsChange;
    private String bounceRateChange;
    private String articlesChange;
    private String period;
    private String contentType;
    private List<Object> performanceTrend;
    private List<Object> engagementByCategory;
    private List<Object> topContent;
}
