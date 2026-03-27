package com.homegenie.gateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficSplitStateTest {

    @Test
    void constructor_setsInitialValues() {
        var state = new TrafficSplitState(30, true);
        assertThat(state.getTrafficPercentage()).isEqualTo(30);
        assertThat(state.isEnabled()).isTrue();
    }

    @Test
    void constructor_clampsInitialPercentageBelowZero() {
        var state = new TrafficSplitState(-10, false);
        assertThat(state.getTrafficPercentage()).isEqualTo(0);
    }

    @Test
    void constructor_clampsInitialPercentageAboveHundred() {
        var state = new TrafficSplitState(150, false);
        assertThat(state.getTrafficPercentage()).isEqualTo(100);
    }

    @Test
    void setTrafficPercentage_updatesValue() {
        var state = new TrafficSplitState(0, false);
        state.setTrafficPercentage(50);
        assertThat(state.getTrafficPercentage()).isEqualTo(50);
    }

    @Test
    void setTrafficPercentage_clampsAtZero() {
        var state = new TrafficSplitState(50, false);
        state.setTrafficPercentage(-5);
        assertThat(state.getTrafficPercentage()).isEqualTo(0);
    }

    @Test
    void setTrafficPercentage_clampsAtHundred() {
        var state = new TrafficSplitState(50, false);
        state.setTrafficPercentage(200);
        assertThat(state.getTrafficPercentage()).isEqualTo(100);
    }

    @Test
    void setTrafficPercentage_boundaryAtZero_allowed() {
        var state = new TrafficSplitState(50, false);
        state.setTrafficPercentage(0);
        assertThat(state.getTrafficPercentage()).isEqualTo(0);
    }

    @Test
    void setTrafficPercentage_boundaryAtHundred_allowed() {
        var state = new TrafficSplitState(50, false);
        state.setTrafficPercentage(100);
        assertThat(state.getTrafficPercentage()).isEqualTo(100);
    }

    @Test
    void setEnabled_togglesFromFalseToTrue() {
        var state = new TrafficSplitState(0, false);
        assertThat(state.isEnabled()).isFalse();
        state.setEnabled(true);
        assertThat(state.isEnabled()).isTrue();
    }

    @Test
    void setEnabled_togglesFromTrueToFalse() {
        var state = new TrafficSplitState(0, true);
        assertThat(state.isEnabled()).isTrue();
        state.setEnabled(false);
        assertThat(state.isEnabled()).isFalse();
    }
}
