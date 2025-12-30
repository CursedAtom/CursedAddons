# Contributing to CursedAddons

Thank you for your interest in contributing to CursedAddons! This document provides guidelines for contributing to the project.

## Getting Started

### Prerequisites
- Java 21 JDK
- Git
- Basic knowledge of Minecraft modding and Fabric

### Development Setup
1. Fork the repository
2. Clone your fork: `git clone https://github.com/yourusername/cursedaddons.git`
3. Set up the development environment: `./gradlew build`
4. Run the mod in a development environment for testing

## Development Guidelines

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods focused and single-purpose
- Handle exceptions appropriately with meaningful error messages

### Commit Messages
- Use clear, descriptive commit messages
- Start with a verb (Add, Fix, Update, Remove, etc.)
- Reference issue numbers when applicable: `Fix #123: resolve chat notification bug`

### Pull Requests
- Create a feature branch from `main`
- Test your changes thoroughly in-game
- Update documentation if needed
- Ensure the build passes: `./gradlew build`
- Provide a clear description of changes

## Feature Requests

### Before Submitting
- Check existing issues to avoid duplicates
- Clearly describe the feature and its benefits
- Consider implementation complexity and scope

### Feature Request Template
Please use the feature request issue template when submitting new feature ideas.

## Bug Reports

### Information to Include
- Minecraft version and Fabric loader version
- Steps to reproduce the issue
- Expected vs. actual behavior
- Any relevant log files or crash reports
- Screenshots if applicable

### Bug Report Template
Use the bug report issue template for consistent reporting.

## Code of Conduct

- Be respectful and constructive in all interactions
- Focus on improving the project and helping others
- Report issues or concerns privately if needed

## Testing

Since this is a Minecraft mod, most testing requires running the game:

- Test in both single-player and multi-player environments
- Verify compatibility with other common mods
- Test edge cases and error conditions
- Performance test with large chat volumes

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (GNU GPL v3.0).
