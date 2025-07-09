# Contributing to Clix Android SDK

Thank you for your interest in contributing to the Clix Android SDK. This document provides guidelines for contributing to the SDK development.

## Development Environment Setup

1. Install Android Studio
2. Fork and clone this repository
3. Open the project in Android Studio

## Code Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- All public APIs must have KDoc documentation
- Use 4 spaces for indentation
- Maximum line length is 120 characters

## Testing

- All new features must include unit tests
- Ensure existing tests don't fail
- Write tests in the `src/test` and `src/androidTest` directories

## Pull Request Process

1. Create a new branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Commit your changes:
   ```bash
   git commit -m "feat: feature description"
   ```
   
   Commit messages should follow this format:
   - feat: New feature
   - fix: Bug fix
   - docs: Documentation changes
   - style: Code formatting
   - refactor: Code refactoring
   - test: Test code
   - chore: Other changes

3. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request on GitHub:
   - PR title should follow the commit message format
   - Provide detailed description of changes
   - Link related issues if any

## Bug Reports

If you find a bug, please report it on GitHub Issues. Include the following information:

- Android version
- Device model
- SDK version
- Steps to reproduce
- Expected vs actual behavior
- Logs or stack traces

## Feature Requests

When suggesting a new feature on GitHub Issues, include:

- Purpose and necessity of the feature
- Possible implementation approaches
- Expected impact

## License

By contributing to this project, you agree that your contributions will be licensed under the MIT License. 