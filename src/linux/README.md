# Linux launcher files

This directory contains files that are committed to git and copied into the Linux distribution during:

```sh
./mvnw -Plinux-dist package
```

`run.sh` is a template. Maven replaces `@APP_JAR@`, `@APP_MODULE@`, and `@APP_MAIN_CLASS@` while assembling `target/YangModelDesigner-1.0-linux.zip`.
