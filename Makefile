LOCAL_NAMESPACE := "olm"

.PHONY: run-local
run-local: build-linux build-wait build-util-linux
	rm -rf build
	. ./scripts/build_local.sh
	mkdir -p build/resources
	. ./scripts/package_release.sh 1.0.0 build/resources doc/install/local-values.yaml
	. ./scripts/install_local.sh $(LOCAL_NAMESPACE) build/resources
	rm -rf build
