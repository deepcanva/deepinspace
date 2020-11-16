package com.canva.services.properties;

import com.canva.services.properties.sort.ArraySorter;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;

public class Main {


  public static void main(String[] args) {
    System.setProperty("spring.profiles.active", System.getProperty("flavor"));

    var context = new AnnotationConfigApplicationContext(LauncherConfiguration.class);
    var configuration = context.getBean(AppConfiguration.class);
    System.out.println(configuration);

    int[] numbers = { 4, 3, 1, 2 };
    context.getBean(ArraySorter.class).sort(numbers);
    System.out.println(Arrays.toString(numbers));
  }
}
