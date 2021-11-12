FROM bczhc/some-tools-build

COPY / /some-tools/

ENTRYPOINT ["/build"]
