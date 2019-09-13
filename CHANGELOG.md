# Changelog

## 3.1.0

- Fix NPM loose comparisons (thanks @kmck)
- Small javadoc fixes

## 3.0.0

- Drop java 6 support
- Add `withSuffix()` and `withBuild()` fluent API (thanks @punkstarman)
- Add pretty string representations for `Range` and `Requirement` (thanks @joschi)
- Add `equals()` and `hashCode()` methods for `Range` and `Requirement` (thanks @joschi)
- Exclude build number from comparison (thanks @astraia)
- Fix `isSatisfiedBy` for loosely built requirements (thanks @sschuberth)

## 2.2.0

This is all @sschuberth huge thanks to him!

- Requirement: Replace build\* methods that take a Semver with a single one
- Fix comparing loosely built requirements
- Semver: Add isGreaterThanOrEqual() and isLowerThanOrEqual() methods
- Set a testSource and testTarget in the Maven config
- Fix coverage reporting to Coveralls

## 2.1.0

- Add support for hyphen signs in build and pre release section (thanks @KristianShishoev)
- Fix a javadoc mistake (thanks @sschuberth)
- Add method Semver#toStrict() to normalize to strict SemVer (thanks @rykov)

## v2.0.3

- Make `Semver.toString` return the same thing as `Semver.getValue`

## v2.0.2

- Handle prerelease conditions in ranges
- Fix invalid operator priority

## v2.0.1

- Fix ranges support for versions with suffix tokens

## v2.0.0

- Java 6 support

## v1.1.0

- Add support for Ivy requirements (used by gradle)

## v1.0.1

- Fix a bug that prevented the usage of `Semver#statisfies(String)` with CocoaPods

## v1.0.0

- Add support for CocoaPods

## v0.2.0

- Add method `Semver#getOriginalValue()`
- Add method `Semver#isStable()`

## v0.1.1

Bug fix on `Semver#getValue()`: it now keeps the original case.

## v0.1.0

First draft of the library.
