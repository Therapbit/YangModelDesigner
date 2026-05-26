# Linux launcher files

This directory contains files that are committed to git and copied into the Linux distribution during:

```sh
./mvnw -Plinux-dist package
```

`run.sh` is copied into the distribution as-is. At startup it searches for `YangModelDesigner-*.jar` next to the script and uses the bundled `lib` directory as the Java module path.
