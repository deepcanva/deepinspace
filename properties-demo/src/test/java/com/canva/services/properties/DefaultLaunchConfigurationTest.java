package com.canva.services.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = LauncherConfiguration.class)
@ActiveProfiles("default")
@RunWith(SpringJUnit4ClassRunner.class)
public class DefaultLaunchConfigurationTest {

  @Autowired
  private GreetingService greetingService;

  @Test
  public void testGreeting() {
    assertThat(greetingService.welcome("James")).isEqualTo("Hello James");
  }
}
