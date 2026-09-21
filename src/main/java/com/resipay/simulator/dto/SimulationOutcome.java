package com.resipay.simulator.dto;

public enum SimulationOutcome {
    SUCCESS,
    DECLINED,
    HTTP_500,
    HTTP_429,
    CONNECTION_RESET,
    TIMEOUT_BEFORE_PROCESSING,
    TIMEOUT_AFTER_PROCESSING
}
