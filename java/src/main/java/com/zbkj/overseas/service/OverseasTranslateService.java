package com.zbkj.overseas.service;

import com.zbkj.overseas.request.TranslateRequest;
import com.zbkj.overseas.response.TranslateResponse;

/**
 * Entry-time name translation (zh -> 11 target languages) for the admin forms.
 * Same contract as the Node implementation's POST /api/admin/translate.
 */
public interface OverseasTranslateService {

    TranslateResponse translate(TranslateRequest request);
}
