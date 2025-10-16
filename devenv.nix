{ pkgs, lib, config, inputs, ... }:

let
  pkgs-unstable = import inputs.nixpkgs-unstable {
    system = pkgs.stdenv.system;
  };
in

{
  name = "zio-llm";

  packages = [
    pkgs.git
  ];

  languages.java = {
    enable = true;
    jdk.package = pkgs-unstable.jdk25_headless;
  };

  languages.scala = {
    enable = true;
    sbt.enable = true;
  };

  env = {
    JAVA_OPTS="--sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED ";
    SBT_OPTS="--sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED ";
  };

  enterShell = ''
    echo "~~~ zio-llm ~~~"
    echo JAVA_HOME=$JAVA_HOME
    export PATH=$PATH
  '';

  enterTest = ''
    sbt test
  '';
}
