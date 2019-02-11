FROM ubuntu
RUN apt-get update \
    && apt-get install -y \
        curl zip unzip bash gcc build-essential libz-dev \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN useradd -ms /bin/bash builds
USER builds
RUN curl -s "https://get.sdkman.io" | /bin/bash \
    && cd \
	&& cat .sdkman/bin/sdkman-init.sh | /bin/bash

ADD install-graal.sh .
RUN ./install-graal.sh

VOLUME [ "/home/builds/src", "/home/builds/.gradle" ]
WORKDIR /home/builds/src
ENTRYPOINT [ "/bin/bash", "/home/builds/src/build/build.sh" ]