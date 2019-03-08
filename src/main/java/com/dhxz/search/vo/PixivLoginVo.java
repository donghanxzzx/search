package com.dhxz.search.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author 10066610
 * @description Pixiv登陆
 * @date 2019/3/8 20:17
 **/
@Data
public class PixivLoginVo {

    private String captcha;
    @JsonProperty("g_recaptcha_response")
    private String gRecaptchaResponse;
    private String password;
    @JsonProperty("pixiv_id")
    private String pixivId;
    @JsonProperty("post_key")
    private String postKey;
    private String source;
    private String ref;
    @JsonProperty("return_to")
    private String returnTo;
}
