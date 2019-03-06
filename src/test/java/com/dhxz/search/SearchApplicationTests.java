package com.dhxz.search;

import com.dhxz.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SearchApplicationTests {

  @Autowired
  private SearchService searchService;
  @Test
  public void contextLoads() {
    //searchService.index();
    searchService.full();

  }
}
