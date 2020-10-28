package com.canva.services.properties.sort;

public class NoOpSort implements ArraySorter{
  {
    System.out.println("NoOp Sort");
  }

  @Override
  public void sort(int[] arr) {

  }
}
