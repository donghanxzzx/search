package com.dhxz.search;

import com.dhxz.search.vo.PixivLoginVo;
import com.dhxz.search.web.utils.ClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author 10066610
 * @description pixiv 相关测试
 * @date 2019/3/8 20:09
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
public class PixivTests {

    @Autowired
    private ClientUtil clientUtil;

    private final String loginUrl = "https://accounts.pixiv.net/login?lang=zh&source=pc&view_type=page&ref=wwwtop_accounts_index";
    private final String index = "https://www.pixiv.net/";

    private final String loginPage = "https://accounts.pixiv.net/login?lang=zh&source=pc&view_type=page&ref=wwwtop_accounts_index";

    @Test
    public void index() {
        Document document = clientUtil.get(index);
        System.out.println(document);
    }

    @Test
    public void login() throws JsonProcessingException, UnsupportedEncodingException {
        PixivLoginVo loginVo = new PixivLoginVo();
        loginVo.setPixivId("donghanxzzx@163.com");
        loginVo.setPassword("donghan19930926");
        loginVo.setPostKey("0cc60123231b0476afbf3595c059d8b9");
        loginVo.setSource("pc");
        loginVo.setRef("wwwtop_accounts_index");
        loginVo.setReturnTo("https://www.pixiv.net/");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(loginVo);

        Document document = clientUtil.postJson(loginUrl, json);

        System.out.println(document);
    }

}
