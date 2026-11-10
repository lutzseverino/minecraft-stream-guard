FROM node:24-bookworm-slim

# Maven wrapper prerequisites and a system-wide JDK; no repository build context.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl unzip gzip tar python3 procps \
    && rm -rf /var/lib/apt/lists/* \
    && case "$(dpkg --print-architecture)" in amd64) jdk_arch=x64 ;; arm64) jdk_arch=aarch64 ;; *) exit 1 ;; esac \
    && jdk_archive="OpenJDK21U-jdk_${jdk_arch}_linux_hotspot_21.0.13_9.tar.gz" \
    && jdk_release="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9" \
    && curl -fLsS -A 'StreamGuard-factory-toolchain/1.0' "$jdk_release/$jdk_archive" -o "/tmp/$jdk_archive" \
    && curl -fLsS -A 'StreamGuard-factory-toolchain/1.0' "$jdk_release/$jdk_archive.sha256.txt" -o /tmp/jdk.sha256 \
    && (cd /tmp && sha256sum -c jdk.sha256) \
    && mkdir -p /opt/java/openjdk \
    && tar -xzf "/tmp/$jdk_archive" -C /opt/java/openjdk --strip-components=1 \
    && rm "/tmp/$jdk_archive" /tmp/jdk.sha256

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:${PATH}"

# The site and delivery checks use the same system-wide package manager as CI.
RUN npm install --global pnpm@11.28.9
