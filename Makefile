# Makefile for Android clix building and code quality

# Define phony targets
.PHONY: build test clean format detekt detekt-baseline all

# Target to build the Android clix (using assemble to potentially skip detekt)
build:
	@echo "Building Clix Android clix..."
	@./gradlew :clix:build

# Target to format Kotlin code using ktfmt
format:
	@echo "Formatting Kotlin code..."
	@./gradlew :clix:ktfmtFormat

# Target to run detekt analysis
detekt:
	@echo "Running detekt analysis..."
	@./gradlew :clix:detekt

# Target to create detekt baseline
detekt-baseline:
	@echo "Creating detekt baseline..."
	@./gradlew :clix:detektBaseline

# Target to run unit tests
test:
	@echo "Running unit tests..."
	@./gradlew :clix:test

# Target to run instrumented tests
test-android:
	@echo "Running Android instrumented tests..."
	@./gradlew :clix:connectedAndroidTest

# Target to clean build files
clean:
	@echo "Cleaning build files..."
	@./gradlew clean

# Target to run both formatting and testing
all: format detekt test
