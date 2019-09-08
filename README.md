## Introduction
A command-line utility to support building a Kubernetes environment on Raspberry PIs

## Prerequisites
This tool is implemented using Ammonite on Ubuntu. In order to execute scripts, checkout Ammonite at (https://ammonite.io). 

For Linux, run the following command to install `amm`:

    sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/lihaoyi/Ammonite/releases/download/1.6.9/2.13-1.6.9) > /usr/local/bin/amm && chmod +x /usr/local/bin/amm' && amm


## Steps to setup a Kubernetes cluster

Follow steps below to setup a Kubernetes cluster remotely from within your workstation

#### Prepare HypriotOS image for SDCards
- Plug SD card into your workstation (assume /dev/sdf)
- Run following command to flash SDCards:

        amm kor.sc flashHypriotOS <your-ssid> <your-wifi-password> <rpi-hostname> <rpi-username>

- Plug SDCards into your RPIs and start them up 

#### Install Kubernetes packages on Raspberry PIs
- Use following command to install dependencies

        amm kor.sc installKubernetes <rpi-hostname> <rpi-username>

#### Create Kubernetes cluster
- Setting up a master node

        amm kor.sc initCluster <rpi-hostname> <rpi-username>

- Setting up worker nodes

        amm kor.sc joinCluster <rpi-hostname> <rpi-username>