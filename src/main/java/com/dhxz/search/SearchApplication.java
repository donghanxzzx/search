package com.dhxz.search;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SearchApplication {

  public static void main(String[] args) {
    SpringApplicationBuilder builder = new SpringApplicationBuilder();
    builder.main(SearchApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
  }
}
