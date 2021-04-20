LOCAL_NAMESPACE := "olm"

.PHONY: run-local
run-local: build-linux build-wait build-util-linux
	rm -rf build
	. ./scripts/build_local.sh
