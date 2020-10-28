package com.canva.services.properties;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class AppConfiguration {
  private final String accessToken;
  private final List<URI> corsOrigins;
  private final int port;
  private final Duration connectionTimeout;
  private final Pattern allowedFrameAncestors;
  @Nullable
  private final String optionalDescription;


  AppConfiguration(String accessToken, List<URI> corsOrigins, int port, Duration connectionTimeout,
      Pattern allowedFrameAncestors, @Nullable String optionalDescription) {
    this.accessToken = accessToken;
    this.corsOrigins = corsOrigins;
    this.port = port;
    this.connectionTimeout = connectionTimeout;
    this.allowedFrameAncestors = allowedFrameAncestors;
    this.optionalDescription = optionalDescription;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accessToken", accessToken)
        .add("corsOrigins", corsOrigins)
        .add("port", port)
        .add("connectionTimeout", connectionTimeout)
        .add("allowedFrameAncestors", allowedFrameAncestors)
        .add("optionalDescription", optionalDescription)
        .toString();
  }

  public static class Builder {
    private String accessToken;
    private List<URI> corsOrigins = List.of();
    private int port = -1;
    private Duration connectionTimeout;
    private Pattern allowedFrameAncestors;
    @Nullable
    private String optionalDescription;

    public Builder setAccessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    public Builder setCorsOrigins(String... corsOrigins) {
      this.corsOrigins = Arrays.stream(corsOrigins) //
          .distinct() //
          .map(uri -> {
            try {
              return new URI(uri);
            } catch (URISyntaxException e) {
              throw new IllegalArgumentException(uri, e);
            }
          }).collect(Collectors.toList());
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setConnectionTimeout(Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public Builder setAllowedFrameAncestors(String allowedFrameAncestors) {
      this.allowedFrameAncestors = Pattern.compile(allowedFrameAncestors);
      return this;
    }

    public Builder setOptionalDescription(@Nullable String optionalDescription) {
      this.optionalDescription = optionalDescription;
      return this;
    }

    public AppConfiguration build() {
      // validation
      Preconditions.checkArgument(port > 0, "Port must be specified");
      Preconditions.checkArgument(!corsOrigins.isEmpty(),
          "At least one CORS origin must be specified");
      return new AppConfiguration(accessToken, corsOrigins, port, connectionTimeout,
          allowedFrameAncestors, optionalDescription);
    }
  }
}
