package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasRfqDao;
import com.zbkj.overseas.model.OverseasRfq;
import com.zbkj.overseas.request.RfqRequest;
import com.zbkj.overseas.service.OverseasRfqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Overseas RFQ service implementation.
 * Rate limiting is a simple per-IP token window (5/min); tjmall spreads this
 * across Redis interceptors — Redis can replace this map unchanged later.
 */
@Slf4j
@Service
public class OverseasRfqServiceImpl extends ServiceImpl<OverseasRfqDao, OverseasRfq>
        implements OverseasRfqService {

    private static final int MAX_PER_MINUTE = 5;
    private final ConcurrentHashMap<String, long[]> hits = new ConcurrentHashMap<>();

    /** Optional webhook fired after a lead is saved (mail/WhatsApp gateway). */
    @Value("${overseas.rfq-webhook-url:}")
    private String rfqWebhookUrl;

    @Override
    public Long submit(RfqRequest request, String clientIp) {
        if (StrUtil.isBlank(request.getContact())) {
            throw new OverseasException(OverseasResultCode.RFQ_CONTACT_REQUIRED);
        }
        checkRateLimit(clientIp == null ? "unknown" : clientIp);

        OverseasRfq rfq = new OverseasRfq();
        rfq.setName(StrUtil.trim(request.getName()));
        rfq.setCompany(StrUtil.trim(request.getCompany()));
        rfq.setCountry(StrUtil.trim(request.getCountry()));
        rfq.setContact(StrUtil.trim(request.getContact()));
        rfq.setProductName(StrUtil.trim(request.getProductName()));
        rfq.setQuantity(StrUtil.trim(request.getQuantity()));
        rfq.setMessage(StrUtil.trim(request.getMessage()));
        rfq.setCreateTime(new Date());
        save(rfq);

        notifyWebhook(rfq);
        return rfq.getId();
    }

    @Override
    public List<OverseasRfq> listLatest(int limit) {
        return list(new LambdaQueryWrapper<OverseasRfq>()
                .orderByDesc(OverseasRfq::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500))));
    }

    private void checkRateLimit(String ip) {
        long now = System.currentTimeMillis();
        long[] slot = hits.compute(ip, (k, v) -> {
            if (v == null || now - v[0] > 60_000) {
                return new long[]{now, 1};
            }
            v[1]++;
            return v;
        });
        if (slot[1] > MAX_PER_MINUTE) {
            throw new OverseasException(OverseasResultCode.RATE_LIMITED);
        }
    }

    private void notifyWebhook(OverseasRfq rfq) {
        if (StrUtil.isBlank(rfqWebhookUrl)) {
            return;
        }
        try {
            HttpUtil.post(rfqWebhookUrl, JSONUtil.toJsonStr(rfq), 5000);
        } catch (Exception e) {
            // Notification failure must never fail the lead capture itself.
            log.warn("RFQ webhook failed for rfq {}: {}", rfq.getId(), e.getMessage());
        }
    }
}
