package com.zbkj.overseas.service;

import com.zbkj.overseas.request.TrackRequest;
import com.zbkj.overseas.response.VisitEventResponse;
import com.zbkj.overseas.response.VisitorStatsResponse;

import java.util.List;

/**
 * Site-wide visitor statistics — same contract as the Node backend.
 * The storefront beacon reports every visitor (not only chat users);
 * agents read the aggregates from the admin console ("visitors" menu
 * permission).
 */
public interface OverseasVisitorService {

    /** Record one beacon hit (view / heartbeat / click); returns the effective visitor token. */
    String track(TrackRequest request, String clientIp, String userAgent);

    /** Admin: summary + country distribution + 7-day trend + latest 100 visitors. */
    VisitorStatsResponse stats();

    /** Admin: one visitor's timeline (views + clicks), newest first. */
    List<VisitEventResponse> events(String token, int limit);
}
