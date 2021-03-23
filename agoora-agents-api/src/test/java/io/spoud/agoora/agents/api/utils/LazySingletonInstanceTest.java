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
    assertThat(inst1.isInstantiated()).isFalse();
    assertThat(inst2.isInstantiated()).isFalse();

    inst1.getInstance();
    assertThat(InstanceCounter.count).isEqualTo(1);
    assertThat(inst1.isInstantiated()).isTrue();
    assertThat(inst2.isInstantiated()).isFalse();

    inst1.getInstance();
    assertThat(InstanceCounter.count).isEqualTo(1);
    assertThat(inst1.isInstantiated()).isTrue();
    assertThat(inst2.isInstantiated()).isFalse();

    inst2.getInstance();
    assertThat(InstanceCounter.count).isEqualTo(2);
    assertThat(inst1.isInstantiated()).isTrue();
    assertThat(inst2.isInstantiated()).isTrue();
  }

  public static class InstanceCounter {
    public static int count = 0;

    public InstanceCounter() {
      count++;
    }
  }
}
