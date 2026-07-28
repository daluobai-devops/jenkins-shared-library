package com.daluobai.jenkinslib.delivery

class DeliveryExecutionException extends RuntimeException {
    final Map deliveryResult

    DeliveryExecutionException(Throwable cause, Map deliveryResult) {
        super(cause.message, cause)
        this.deliveryResult = deliveryResult
    }
}
