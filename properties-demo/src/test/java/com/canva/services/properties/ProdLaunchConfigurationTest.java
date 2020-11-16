package com.canva.services.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.canva.services.properties.sort.ArraySorter;
import com.canva.services.properties.sort.QuickSort;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = LauncherConfiguration.class)
@ActiveProfiles("prod")
@RunWith(SpringJUnit4ClassRunner.class)
public class ProdLaunchConfigurationTest {

  @Autowired
  private AppConfiguration configuration;

  @Autowired
  private ArraySorter arraySorter;

  @Test
  public void checkConfiguration() {
    assertThat(configuration.port).isEqualTo(8080);
  }

  @Test
  public void checkSorter() {
    assertThat(arraySorter).isInstanceOf(QuickSort.class);

    int[] sample = { 3, 2, 1 };
    arraySorter.sort(sample);
    assertThat(sample).containsExactly(1 , 2, 3);
  }
}
