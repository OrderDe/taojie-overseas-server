package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.model.OverseasRfq;
import com.zbkj.overseas.request.RfqRequest;

import java.util.List;

/**
 * Overseas RFQ service.
 */
public interface OverseasRfqService extends IService<OverseasRfq> {

    /** Save a lead, then fire the optional webhook notification (RFQ notify hook). */
    Long submit(RfqRequest request, String clientIp);

    /** Admin: latest leads. */
    List<OverseasRfq> listLatest(int limit);
}
