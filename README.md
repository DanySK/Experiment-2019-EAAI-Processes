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

### Automatic releases

Charts are remotely generated and made available on the project release page.
[The latest release](https://github.com/DanySK/Experiment-2019-EAAI-Processes/releases/latest) allows for quick retrieval of the latest version of the charts.

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

### Python environment configuration

The following guide will start from the assumption that pyenv is installed on your system.
First, install Python by issuing

``pyenv install --skip-existing 3.6.3``

Now, configure the project to be interpreted with exactly that version:

``pyenv local 3.6.3``

In order not to dirt your working environment, we recommend using a virtual environment:

```bash
python -m venv venv
source venv/bin/activate
```

Your shell should be now configured to run in a virtual environment.
Update the `pip` package manager and install the required dependencies.

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

If the process completes successfully, your local environment is ready for executing the data crunching script.
You can exit the virtual environment mode by issuing ``deactivate``

### Data processing and chart creation

This section assumes you correctly completed the required configuration described in the previous section.
In order for the script to execute, you only need to:

1. Enter the virtual environment with `source venv/bin/activate`
2. Launch the actual process by issuing `python process.py`

If you do not need to use the script any longer and want to get back to your normal shell, issue `deactivate`

## Implementation details

The simulation project is organised as follows:

- `src/main/yaml/service_discovery.yml` is the **simulation descriptor**: it specifies simulation variables, molecules to be exported, the network model (distance-based connectivity) and structure (circular arcs modelling a hierarchical network), and the programs to be executed
- `src/main/resources/service_discovery.aes`: file of the effects for the graphical simulation
- `src/main/scala/it/unibo/casestudy/`: directory with the Scala/ScaFi source code, organised as follows
    - `ServiceDomainModel.scala`: implements the domain model of the case study in terms of interfaces and data classes (for entities like *services* and *tasks*)
    - `TaskGenerator.scala`: program executed with `taskFrequency` (`ExponentialTime` distribution) in order to simulate the arrival of new tasks to be executed
    - `ServiceDiscovery.scala`: the core aggregate program of the case study, augmented with simulation-specific code
        - notice that the `ServiceDiscovery` class extends `AggregateProgram` and mixes aggregate libraries in
        - the entry point is the `main` method of the `ServiceDiscovery` class
        - the baseline algorithm is given by method `classicServiceDiscovery`
        - the aggregate process-based algorithm is given by method `processBasedServiceDiscovery`,
        which is quite simple as much of the logic is in the process definition method `serviceDiscoveryProcess`
