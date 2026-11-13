package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Payload of GET /api/admin/visitors — same shape as the Node backend:
 * summary cards, per-country distribution, 7-day trend, latest rows.
 */
@Data
@Schema(name = "VisitorStatsResponse", description = "访客统计总览")
public class VisitorStatsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Summary summary;

    private List<CountryCount> countries;

    private List<TrendDay> trend;

    private List<VisitorRowResponse> visitors;

    @Data
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Visitors seen within the last 60 seconds. */
        private Integer online;
        private Integer todayUv;
        private Integer todayPv;
        private Integer totalUv;
        private Integer totalPv;
    }

    @Data
    public static class CountryCount implements Serializable {
        private static final long serialVersionUID = 1L;
        private String country;
        private Integer visitors;
    }

    @Data
    public static class TrendDay implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Local calendar date, yyyy-MM-dd. */
        private String date;
        private Integer uv;
        private Integer pv;
    }
}
