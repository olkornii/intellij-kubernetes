 #!/usr/bin/env bash

# Note: run from root
# This is used to run and connect minikube cluster

set -e

[ -x "$(command -v kind)" ] && [["$(kubectl config current-context)" =~ ^kind-?]] && NO_MINIKUBE=1

if [ -z "$NO_MINIKUBE" ]; then
  pgrep -f "[m]inikube" >/dev/null || minikube start --kubernetes-version="v1.16.4" --extra-config=apiserver.v=4 || { echo 'Cannot start minikube.'; exit 1; }
  eval "$(minikube docker-env)" || { echo 'Cannot switch to minikube docker'; exit 1; }
  kubectl config use-context minikube
fi

# Check if cluster is in config, if not - here will be the empty labels
kubectl config view
