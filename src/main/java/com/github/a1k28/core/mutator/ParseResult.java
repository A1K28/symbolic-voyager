package com.github.a1k28.core.mutator;

import org.pitest.mutationtest.config.ReportOptions;

import java.util.Optional;

public class ParseResult {
  private final ReportOptions options;
  private final Optional<String> errorMessage;

  public ParseResult(final ReportOptions options, final String errorMessage) {
    this.options = options;
    this.errorMessage = Optional.ofNullable(errorMessage);
  }

  public boolean isOk() {
    return !this.errorMessage.isPresent();
  }

  public ReportOptions getOptions() {
    return this.options;
  }

  public Optional<String> getErrorMessage() {
    return this.errorMessage;
  }

}
