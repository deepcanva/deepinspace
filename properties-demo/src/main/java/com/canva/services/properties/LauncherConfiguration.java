package com.canva.services.properties;

import com.canva.services.annotations.DevConfiguration;
import com.canva.services.annotations.ProdConfiguration;
import com.canva.services.properties.sort.ArraySorter;
import com.canva.services.properties.sort.NoOpSort;
import com.canva.services.properties.sort.QuickSort;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LauncherConfiguration {

  public static void main(String[] args) {
    System.setProperty("spring.profiles.active", System.getProperty("flavor"));

    var context = new AnnotationConfigApplicationContext(LauncherConfiguration.class);
    AppConfiguration configuration = context.getBean(AppConfiguration.class);
    System.out.println(configuration);
    System.out.println("");
  }


  @DevConfiguration
  static class DevAppConfig {
    @Bean
    AppConfiguration appConfiguration() {
      return new AppConfiguration.Builder() //
          .setAccessToken("D3123456")
          .setAllowedFrameAncestors("http://dev.+/(m|b|c)at")
          .setPort(8081)
          .setCorsOrigins("http://www.canva-dev.com")
          .setOptionalDescription(null)
          .build();
    }

    @Bean
    ArraySorter arraySorter() {
      return new NoOpSort();
    }
  }

  @ProdConfiguration
  static class ProdAppConfig {
    @Bean
    public AppConfiguration appConfiguration() {
      return new AppConfiguration.Builder() //
          .setAccessToken("P0123456")
          .setAllowedFrameAncestors("http://.+/(m|b|c)at")
          .setPort(8080)
          .setCorsOrigins("http://www.canva.com")
          .setOptionalDescription("Good stuff")
          .build();
    }

    @Bean
    ArraySorter arraySorter() {
      return new QuickSort();
    }
  }
}
