LOCAL_NAMESPACE := "olm"

ORG := github.com/operator-framework
PKG   := $(ORG)/operator-lifecycle-manager

MOD_FLAGS := $(shell (go version | grep -q -E "1\.1[1-9]") && echo -mod=vendor)
CMDS  := $(shell go list $(MOD_FLAGS) ./cmd/...)

build-linux: build_cmd=build
build-linux: arch_flags=GOOS=linux GOARCH=386
build-linux: clean $(CMDS)

build-wait: clean bin/wait

bin/wait: FORCE
	GOOS=linux GOARCH=386 go build $(MOD_FLAGS) -o $@ $(PKG)/test/e2e/wait

build-util-linux: arch_flags=GOOS=linux GOARCH=386
build-util-linux: build-util

build-util: bin/cpb

bin/cpb: FORCE
	CGO_ENABLED=0 $(arch_flags) go build $(MOD_FLAGS) -ldflags '-extldflags "-static"' -o $@ ./util/cpb

$(CMDS): version_flags=-ldflags "-X $(PKG)/pkg/version.GitCommit=$(GIT_COMMIT) -X $(PKG)/pkg/version.OLMVersion=`cat OLM_VERSION`"
$(CMDS):
	$(arch_flags) go $(build_cmd) $(MOD_FLAGS) $(version_flags) -tags "json1" -o bin/$(shell basename $@) $@

# Phony prerequisite for targets that rely on the go build cache to determine staleness.
.PHONY: FORCE
FORCE:

clean:
	@rm -rf cover.out
	@rm -rf bin
	@rm -rf test/e2e/resources
	@rm -rf test/e2e/test-resources
	@rm -rf test/e2e/log
	@rm -rf e2e.namespace

.PHONY: run-local
run-local: build-linux build-wait build-util-linux
	rm -rf build
	. ./scripts/build_local.sh
