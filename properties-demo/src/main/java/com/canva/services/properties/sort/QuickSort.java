package com.canva.services.properties.sort;

import java.util.Arrays;

public class QuickSort implements ArraySorter {
  {
    System.out.println("Quick Sort");
  }

  @Override
  public void sort(int[] arr) {
    Arrays.sort(arr);
  }
}
