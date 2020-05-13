package com.elminster.jcp.ast;

public interface Engine {

  void start();

  /**
   * Resume from the step.
   */
  void resume();

  void stop();

  void restart();
}
