# Scripting Rules

## Intro
This plugin is an experimentation, and as such, no guarantees are offered regarding its future.

The objective of this plugin is to simplify the step of defining custom submit rules for project
owners who don't have administrative privileges. This repository contains a framework making it
easier to write **scripting engines**, but the exact definition of an engine is still blurry.
Ideally, writing a Prolog engine from scratch should be easier thanks to this plugin.

The first engine provided will allow rules to be written in **JavaScript**. Communication between
the rules and the users relies on submit requirements.

## Developer's toolbox
This project relies on the Bazel build system, just like the rest of the Gerrit project.

In order to use the code of this plugin, clone the project in your local clone of Gerrit, inside of
the `plugins/scripting-rules` directory. You then need to copy the `external_plugin_deps.bzl` file
from this repository inside the `plugins/` directory.

```
~/gerrit# cd plugins
# Clone the project
~/gerrit/plugins# git clone https://gerrit.googlesource.com/plugins/scripting-rules
~/gerrit/plugins# cd scripting-rules
# Copy the external_plugin_deps.bzl file
~/gerrit/plugins/scripting-rules# cp external_plugin_deps.bzl ../
# Setup the Change-Id git hook.
~/gerrit/plugins/scripting-rules# f=`git rev-parse --git-dir`/hooks/commit-msg
~/gerrit/plugins/scripting-rules# curl -Lo $f https://gerrit-review.googlesource.com/tools/hooks/commit-msg
~/gerrit/plugins/scripting-rules# chmod +x $f
```

### Compile
To build this projecy, use the `bazel build //plugins/scripting-rules` command.

```
~/gerrit # bazel build //plugins/scripting-rules
Starting local Bazel server and connecting to it...
...........
INFO: Analysed target //plugins/scripting-rules:scripting-rules (169 packages loaded).
INFO: Found 1 target...
Target //plugins/scripting-rules:scripting-rules up-to-date:
  bazel-genfiles/plugins/scripting-rules/scripting-rules.jar
INFO: Elapsed time: 11.823s, Critical Path: 3.97s
INFO: 82 processes: 77 remote cache hit, 3 linux-sandbox, 2 worker.
INFO: Build completed successfully, 90 total actions
```

The target is the plugin's jar file, in this case it is stored in
`bazel-genfiles/plugins/scripting-rules/scripting-rules.jar`.

### Test
To run all the tests, use the `bazel test //plugins/scripting-rules/...` command.

```asciidoc
~/gerrit # bazel test //plugins/scripting-rules/...
INFO: Analysed 2 targets (76 packages loaded).
INFO: Found 2 test targets...
INFO: Elapsed time: 2.823s, Critical Path: 1.43s
INFO: 40 processes: 35 remote cache hit, 3 linux-sandbox, 2 worker.
INFO: Build completed successfully, 43 total actions
//plugins/scripting-rules/javatests/com/googlesource/gerrit/plugins/scripting/rules/engines:engines PASSED in 0.2s
//plugins/scripting-rules/javatests/com/googlesource/gerrit/plugins/scripting/rules/utils:utils PASSED in 0.5s

Executed 2 out of 2 tests: 2 tests pass.
INFO: Build completed successfully, 43 total actions
```

### Adding an engine
Engines are defined in the
`plugins/scripting-rules/java/com/googlesource/gerrit/plugins/scripting/rules/engines/` directory,
and must implement the `com.googlesource.gerrit.plugins.scripting.rules.engines.RuleEngine` class.

The engine name (which is the directory name) should also be enabled in the `plugin.bzl` file. This
extra step makes it easier to enable or completely disable engines on the fly.

In order to be used, the Engine must be declared in the `EnginesModule` file, either by installing a
module or by adding the engine to the DynamicSet:
`DynamicSet.bind(binder(), RuleEngine.class).to(MyEngineName.class);`
