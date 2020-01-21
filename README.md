# Engineering Collective Intelligence at the Edge with Aggregate Processes

## Experimental results of the third case study

This repository contains code and instruction to reproduce the experiments presented in the paper "Engineering Collective Intelligence at the Edge with Aggregate Processes" by Roberto Casadei, Danilo Pianini, Mirko Viroli, Giorgio Audrito, and Ferruccio Damiani; submitted to Elsevier's [Engineering Applications of Artificial Intelligence](https://www.journals.elsevier.com/engineering-applications-of-artificial-intelligence) journal.

## Requirements

In order to run the experiments, the JDK11 is required.
It is very likely that they can run on any later version, but it is not guaranteed.
We recommend using OpenJDK11, which was used for the original testing.

In order to produce the charts, Python 3 is required.
We recommend Python 3.6.3, but it is very likely that any subsequent version will serve the purpose just as well.
The recommended way to obtain it is via [pyenv](https://github.com/pyenv/pyenv).

The experiments have been designed and tested under Linux.
However, all the tools used are multiplatform, and so there is a good chance that the software suite can run on MacOS X, and possibly under Windows.

### Reference machine

We provide a reference Travis CI configuration to maintain reproducibility over time.
While this image: [![Build Status](https://travis-ci.org/DanySK/Experiment-2019-EAAI-Processes.svg?branch=master)](https://travis-ci.org/DanySK/Experiment-2019-EAAI-Processes)
is green, it means that the experiment is being maintained and that, by copying the Travis CI configuration found in the `.travis.yml` file, you should be able to re-run the tests entirely.

## Running the simulations

A graphical execution of the simulation can be started by issuing the following command
`./gradlew runService_discovery`.
Windows users may try using the `gradlew.bat` script as a replacement for `gradlew`.

The whole simulation batch can be executed by issuing `./gradlew sim`. **Be aware that it may take a very long time**, from several hours to weeks, depending on your hardware.
If you are under Linux, the system tries to detect the available memory and CPUs automatically, and parallelize the work.

## Generating the charts

In order to speed up the process for those interested in observing and manipulating the existing data, we provide simulation-generated data directly in the repository.
Generating the charts is matter of executing the `process.py` script.
The enviroment is designed to be used in conjunction with pyenv.
The following guide will start from the assumption that pyenv is installed on your system.

First, install Python by issuing ``pyenv install --skip-existing 3.6.3``

