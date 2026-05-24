package com.payhub.core.controls;

/**
 * Common SPI contract for all payment flow controls. Each control implementation is discovered via
 * {@link java.util.ServiceLoader} and wired with its required ports by {@link
 * com.payhub.core.controls.ControlInjector}.
 *
 * @param <I> input type for this control
 * @param <O> output type returned by this control
 */
public interface Control<I, O> {

  O execute(I input);
}
