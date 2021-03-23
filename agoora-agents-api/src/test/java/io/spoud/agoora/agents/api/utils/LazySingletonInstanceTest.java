package io.spoud.agoora.agents.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LazySingletonInstanceTest {

  @Test
  void testLazyInit() {
    final LazySingletonInstance<InstanceCounter> inst1 =
        new LazySingletonInstance<>(InstanceCounter::new);
    final LazySingletonInstance<InstanceCounter> inst2 =
        new LazySingletonInstance<>(InstanceCounter::new);

    assertThat(InstanceCounter.count).isZero();

    inst1.getInstance();
    assertThat(InstanceCounter.count).isEqualTo(1);

    inst1.getInstance();
    assertThat(InstanceCounter.count).isEqualTo(1);

    inst2.getInstance();
    assertThat(InstanceCounter.count).isEqualTo(2);
  }

  public static class InstanceCounter {
    public static int count = 0;

    public InstanceCounter() {
      count++;
    }
  }
}
