# Changelog

## v2.0.1

* Fix ranges support for versions with suffix tokens

## v2.0.0

* Java 6 support

## v1.1.0

* Add support for Ivy requirements (used by gradle)

## v1.0.1

* Fix a bug that prevented the usage of `Semver#statisfies(String)` with CocoaPods

## v1.0.0

* Add support for CocoaPods

## v0.2.0

* Add method `Semver#getOriginalValue()`
* Add method `Semver#isStable()`

## v0.1.1

Bug fix on `Semver#getValue()`: it now keeps the original case.

## v0.1.0

First draft of the library.
