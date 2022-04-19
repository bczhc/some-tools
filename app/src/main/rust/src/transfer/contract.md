# Transfer

## Send

| Header (8) | Mark (1) | FileNameLength (4, BE) | FileName | Message |
| :--------: | :------- | ---------------------- | -------- | ------- |

## Mark

- **FILE**: 1
- **TEXT**: 2
- **TAR**: 3

[//]: # "- **EOF**: 4"

When `Mark` is not `FILE`, `FileNameLength` is 0, and `FileName` is not presented.

